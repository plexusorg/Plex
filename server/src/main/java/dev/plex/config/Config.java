package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Creates a custom Config object
 */
public class Config extends YamlConfiguration
{
    /**
     * The plugin instance
     */
    private final Plex plugin;

    /**
     * The File instance
     */
    private final File file;

    /**
     * The file name
     */
    private final String name;

    /**
     * Creates a config object
     *
     * @param plugin The plugin instance
     * @param name   The file name
     */
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

    /**
     * Loads the configuration file
     */
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
        plugin.saveResource(name, false);
    }
}
