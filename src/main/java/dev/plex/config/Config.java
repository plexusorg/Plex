package dev.plex.config;

import dev.plex.Plex;
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
    private Plex plugin;
    /**
     * The File instance
     */
    private File file;

    /**
     * The file name
     */
    private String name;

    /**
     * Creates a config object
     * @param plugin The plugin instance
     * @param name The file name
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

    /**
     * Loads the configuration file
     */
    public void load()
    {
        try
        {
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