package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.error.YAMLException;

public class Config extends YamlConfiguration
{
    private final Plex plugin;
    private final File file;
    private final String name;
    public Config(Plex plugin, String name)
    {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), name);
        this.name = name;

        if (!file.exists())
        {
            saveDefault();
        }
    }

    public void load()
    {
        this.load(true);
    }

    // Holds the monitor across the file read, so a reload cannot replace a newer saveChange with older bytes.
    public synchronized void load(boolean reconcileWithDefaults)
    {
        try
        {
            if (reconcileWithDefaults)
            {
                ConfigDefaultsMerger.Result result = ConfigDefaultsMerger.merge(file, plugin.getResource(name), name);
                if (!result.addedKeys().isEmpty())
                {
                    PlexLog.log("Merged default key(s) into " + name + ": " + String.join(", ", result.addedKeys()));
                }
            }

            this.options().parseComments(true);
            super.load(file);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Could not load configuration " + name, ex);
        }
    }

    @Override
    public synchronized void loadFromString(String contents) throws InvalidConfigurationException
    {
        if (name.equals("indefbans.yml"))
        {
            validateIndefiniteBanLabels(contents);
        }
        super.loadFromString(contents);
        if (name.equals("worlds.yml"))
        {
            plugin.getWorldModificationPolicy().reload(this);
        }
    }

    private static void validateIndefiniteBanLabels(String contents) throws InvalidConfigurationException
    {
        // Inspect the original keys before Paper interprets dots as configuration paths.
        LoaderOptions options = new LoaderOptions();
        options.setAllowDuplicateKeys(false);
        Object document;
        try
        {
            document = new Yaml(new SafeConstructor(options)).load(contents);
        }
        catch (YAMLException ex)
        {
            throw new InvalidConfigurationException("Invalid indefbans.yml: " + ex.getMessage(), ex);
        }
        if (document == null)
        {
            return;
        }
        if (!(document instanceof Map<?, ?> entries))
        {
            throw new InvalidConfigurationException("indefbans.yml must contain labeled ban blocks.");
        }
        Set<String> labels = new HashSet<>();
        for (Map.Entry<?, ?> entry : entries.entrySet())
        {
            String label = String.valueOf(entry.getKey());
            if (!isValidIndefiniteBanLabel(label))
            {
                throw new InvalidConfigurationException("Invalid indefinite ban label '" + label + "': labels must be non-empty and cannot contain dots.");
            }
            if (!labels.add(label))
            {
                throw new InvalidConfigurationException("Duplicate indefinite ban label '" + label + "'.");
            }
            if (!(entry.getValue() instanceof Map<?, ?>))
            {
                throw new InvalidConfigurationException("Indefinite ban label '" + label + "' must contain a ban block.");
            }
        }
    }

    // A dot would split the label into a nested Bukkit configuration path.
    public static boolean isValidIndefiniteBanLabel(String label)
    {
        return !label.isEmpty() && !label.contains(".");
    }

    public void save()
    {
        try
        {
            super.save(file);
        }
        catch (Exception ex)
        {
            throw new IllegalStateException("Could not save configuration " + name, ex);
        }
    }

    public record Change<T>(boolean written, T reloaded) { }

    // Applies the change to the contents of the file as they are on disk, so edits not yet reloaded are kept, then
    // loads the result and computes the reload result from it. When the change returns empty, the file is left as it
    // is and the contents read are loaded instead, so memory matches the file either way. The file is resolved through
    // symbolic links, so a linked file is replaced where it lives. It is replaced by renaming a synced temporary file
    // with its permissions and, where the server user may set it, its group over it, so a crash leaves either the old
    // or the new contents. If the read, the change, the load, the reload, or the write fails, the file is untouched
    // and the previous contents are restored in memory. Once the rename succeeds the change is saved; the directory is
    // then synced so the rename survives power loss, and a failed sync is logged.
    public synchronized <T> Change<T> saveChange(Function<String, Optional<String>> change, Supplier<T> reload)
    {
        String previous = saveToString();
        Path target;
        Path temporary = null;
        T result;
        try
        {
            target = file.toPath().toRealPath();
            String current;
            try
            {
                current = Files.readString(target, StandardCharsets.UTF_8);
            }
            catch (CharacterCodingException failure)
            {
                throw new IOException(name + " is not valid UTF-8 and must be re-saved as UTF-8.", failure);
            }
            Optional<String> contents = change.apply(current);
            loadFromString(contents.orElse(current));
            result = reload.get();
            if (contents.isEmpty())
            {
                return new Change<>(false, result);
            }
            // A unique name, so servers sharing the file through links cannot write the same temporary file.
            temporary = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
            Files.writeString(temporary, contents.get(), StandardCharsets.UTF_8);
            PosixFileAttributeView attributes = Files.getFileAttributeView(target, PosixFileAttributeView.class);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE))
            {
                // Set while the channel is open, so the sync covers them even when the mode makes the file read-only.
                // The group is set before the mode, because a group change can clear the setuid and setgid bits.
                if (attributes != null)
                {
                    PosixFileAttributes original = attributes.readAttributes();
                    try
                    {
                        Files.getFileAttributeView(temporary, PosixFileAttributeView.class).setGroup(original.group());
                    }
                    catch (IOException failure)
                    {
                        PlexLog.warn("Could not give the new " + name + " the group " + original.group().getName()
                                + " of the file it replaces, so it is saved with the server user's group.", failure);
                    }
                    Files.setPosixFilePermissions(temporary, original.permissions());
                }
                channel.force(true);
            }
            try
            {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (FileSystemException failure)
            {
                throw new IOException("Could not replace " + target + " by renaming a temporary file over it. A file "
                        + "mounted on its own, such as a single-file bind mount, cannot be replaced this way; mount its "
                        + "directory instead.", failure);
            }
        }
        catch (IOException | InvalidConfigurationException | RuntimeException failure)
        {
            try
            {
                loadFromString(previous);
            }
            catch (InvalidConfigurationException restoreFailure)
            {
                failure.addSuppressed(restoreFailure);
            }
            if (temporary != null)
            {
                try
                {
                    Files.deleteIfExists(temporary);
                }
                catch (IOException deleteFailure)
                {
                    failure.addSuppressed(deleteFailure);
                }
            }
            throw new IllegalStateException("Could not save configuration " + name, failure);
        }
        // Windows cannot open a directory as a channel; NTFS journals the rename.
        if (!System.getProperty("os.name").startsWith("Windows"))
        {
            try (FileChannel directory = FileChannel.open(target.getParent(), StandardOpenOption.READ))
            {
                directory.force(true);
            }
            catch (IOException failure)
            {
                PlexLog.warn("Saved " + name + ", but could not sync its directory, so a power loss may undo the change.",
                        failure);
            }
        }
        return new Change<>(true, result);
    }

    private void saveDefault()
    {
        plugin.saveResource(name, false);
    }
}
