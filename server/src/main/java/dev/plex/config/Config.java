package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

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
     * Whether new entries were added to the file automatically
     */
    private boolean added = false;

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
    public void load(boolean loadFromFile)
    {
        try
        {
            if (loadFromFile)
            {
                YamlConfiguration externalYamlConfig = YamlConfiguration.loadConfiguration(file);
                InputStreamReader internalConfigFileStream = new InputStreamReader(plugin.getResource(name), StandardCharsets.UTF_8);
                YamlConfiguration internalYamlConfig = YamlConfiguration.loadConfiguration(internalConfigFileStream);

                // Gets all the keys inside the internal file and iterates through all of it's key pairs
                for (String string : internalYamlConfig.getKeys(true))
                {
                    // Checks if the external file contains the key already.
                    if (!externalYamlConfig.contains(string))
                    {
                        // If it doesn't contain the key, we set the key based off what was found inside the plugin jar
                        externalYamlConfig.setComments(string, internalYamlConfig.getComments(string));
                        externalYamlConfig.setInlineComments(string, internalYamlConfig.getInlineComments(string));
                        externalYamlConfig.set(string, internalYamlConfig.get(string));
                        PlexLog.log("Setting key: " + string + " in " + this.name + " to the default value(s) since it does not exist!");
                        added = true;
                    }
                }
                if (added)
                {
                    externalYamlConfig.save(file);
                    PlexLog.log("Saving new file...");
                    added = false;
                }
            }
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