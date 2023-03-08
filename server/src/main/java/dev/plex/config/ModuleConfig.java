package dev.plex.config;

import dev.plex.module.PlexModule;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Creates a custom Config object
 */
public class ModuleConfig extends YamlConfiguration
{
    /**
     * The plugin instance
     */
    private PlexModule module;

    /**
     * The File instance
     */
    private File file;

    /**
     * Where the file is in the module JAR
     */
    private String from;

    /**
     * Where it should be copied to in the module folder
     */
    private String to;

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
            Files.copy(module.getClass().getResourceAsStream("/" + from), this.file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}