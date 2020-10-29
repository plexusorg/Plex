package me.totalfreedom.plex.config;

import java.io.File;
import me.totalfreedom.plex.Plex;
import org.bukkit.configuration.file.YamlConfiguration;

public class MainConfig extends YamlConfiguration
{
    private static MainConfig config;
    private final Plex plugin;
    private final File file;

    public MainConfig(Plex plugin)
    {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "config.yml");

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

    private void saveDefault()
    {
        plugin.saveResource("config.yml", false);
    }
}