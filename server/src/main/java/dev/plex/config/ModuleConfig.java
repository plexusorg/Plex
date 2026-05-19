package dev.plex.config;

import dev.plex.module.PlexModule;
import dev.plex.util.PlexLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Creates a custom Config object
 */
public class ModuleConfig extends YamlConfiguration
{
    /**
     * The plugin instance
     */
    private final PlexModule module;

    /**
     * The File instance
     */
    private final File file;

    /**
     * Where the file is in the module JAR
     */
    private final String from;

    /**
     * Where it should be copied to in the module folder
     */
    private final String to;

    /**
     * Creates a config object
     *
     * @param module The module instance
     * @param to     The file name
     */
    public ModuleConfig(PlexModule module, String from, String to)
    {
        this.module = module;
        this.file = new File(module.getDataFolder(), to);
        this.to = to;
        this.from = from;

        if (!file.exists())
        {
            saveDefault();
        }
    }

    public void load()
    {
        try
        {
            ConfigDefaultsMerger.Result result = ConfigDefaultsMerger.merge(file, module.getClass().getResourceAsStream("/" + from), to);
            if (!result.addedKeys().isEmpty())
            {
                PlexLog.log("Merged default key(s) into " + to + ": " + String.join(", ", result.addedKeys()));
            }

            this.options().parseComments(true);
            super.load(file);
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Saves the configuration file
     */
    public void save()
    {
        try
        {
            super.save(file);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Moves the configuration file from the plugin's resources folder to the data folder (plugins/Plex/)
     */
    private void saveDefault()
    {
        try
        {
            File parent = file.getParentFile();
            if (parent != null)
            {
                parent.mkdirs();
            }
            try (InputStream stream = module.getClass().getResourceAsStream("/" + from))
            {
                if (stream == null)
                {
                    PlexLog.warn("Unable to save default module config " + to + ": missing resource " + from);
                    return;
                }
                Files.copy(stream, this.file.toPath());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
