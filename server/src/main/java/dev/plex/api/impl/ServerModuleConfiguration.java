package dev.plex.api.impl;

import dev.plex.api.config.ModuleConfiguration;
import dev.plex.config.ConfigDefaultsMerger;
import dev.plex.module.PlexModule;
import dev.plex.util.PlexLog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.configuration.InvalidConfigurationException;

final class ServerModuleConfiguration extends ModuleConfiguration
{
    private final PlexModule module;
    private final File file;
    private final String fileName;

    ServerModuleConfiguration(PlexModule module, String fileName)
    {
        this.module = Objects.requireNonNull(module, "module");
        Objects.requireNonNull(fileName, "fileName");
        if (fileName.isBlank())
        {
            throw new IllegalArgumentException("Module configuration path must not be blank");
        }
        Path relativePath = Path.of(fileName).normalize();
        if (relativePath.isAbsolute() || relativePath.getNameCount() == 0 || relativePath.startsWith(".."))
        {
            throw new IllegalArgumentException("Module configuration path must stay in the module data folder");
        }
        Path dataFolder = module.getDataFolder().toPath().toAbsolutePath().normalize();
        Path configPath = dataFolder.resolve(relativePath).normalize();
        if (configPath.equals(dataFolder) || !configPath.startsWith(dataFolder))
        {
            throw new IllegalArgumentException("Module configuration path must stay in the module data folder");
        }
        this.file = configPath.toFile();
        this.fileName = relativePath.toString().replace('\\', '/');
        if (!file.exists()) saveDefault();
    }

    @Override
    public void load()
    {
        try
        {
            ConfigDefaultsMerger.Result result = ConfigDefaultsMerger.merge(file, module.getResource(fileName), fileName);
            if (!result.addedKeys().isEmpty())
            {
                PlexLog.log("Merged default key(s) into " + fileName + ": " + String.join(", ", result.addedKeys()));
            }
            options().parseComments(true);
            super.load(file);
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void save()
    {
        try { super.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void saveDefault()
    {
        try
        {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try (InputStream stream = module.getResource(fileName))
            {
                if (stream == null)
                {
                    PlexLog.warn("Unable to save default module config " + fileName + ": missing resource " + fileName);
                    return;
                }
                Files.copy(stream, file.toPath());
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
