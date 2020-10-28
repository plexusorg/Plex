package me.totalfreedom.plex.config;

import java.io.File;
import me.totalfreedom.plex.Plex;
import org.bukkit.configuration.file.YamlConfiguration;

public class MainConfig extends YamlConfiguration
{
    private static MainConfig config;
    private final Plex plugin;
    private final File file;

    public MainConfig(Plex plugin, String configName)
    {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), configName);

        if (!file.exists())
        {
            saveDefault(configName);
        }
    }

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

    private void saveDefault(String configName)
    {
        plugin.saveResource(configName, false);
    }
}