package me.totalfreedom.plex.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class YamlConfig
{

    private YamlConfiguration config;
    private File file;

    public YamlConfig(File file)
    {
        this.file = file;
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public YamlConfiguration get() {
        return config;
    }

    public void save()
    {
        try {
            this.config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reload()
    {
        try {
            this.config.load(file);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }
}
