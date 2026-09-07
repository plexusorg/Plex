package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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

    public void load(boolean reconcileWithDefaults)
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
    public void loadFromString(String contents) throws InvalidConfigurationException
    {
        if (name.equals("indefbans.yml"))
        {
            validateIndefiniteBanLabels(contents);
        }
        super.loadFromString(contents);
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
            if (label.isEmpty() || label.contains("."))
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

    private void saveDefault()
    {
        plugin.saveResource(name, false);
    }
}
