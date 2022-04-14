package dev.plex.config;

import dev.plex.module.PlexModule;
import java.io.File;
import java.io.IOException;
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
    private PlexModule module;

    /**
     * The File instance
     */
    private File file;

    /**
     * The file name
     */
    private String name;

    /**
     * The folder in which the module files are in
     */
    private String folder;

    /**
     * Creates a config object
     *
     * @param module The module instance
     * @param name   The file name
     */
    public ModuleConfig(PlexModule module, String name, String folder)
    {
        this.module = module;
        this.file = new File(module.getDataFolder(), name);
        this.name = name;
        this.folder = folder;

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
            Files.copy(module.getClass().getResourceAsStream("/" + folder), this.file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}