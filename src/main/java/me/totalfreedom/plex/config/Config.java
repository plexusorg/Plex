package me.totalfreedom.plex.config;

import java.io.File;
import me.totalfreedom.plex.Plex;
import org.bukkit.configuration.file.YamlConfiguration;

public class Config extends YamlConfiguration
{
    private Plex plugin;
    private File file;
    private String name;

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
        plugin.saveResource(name, false);
    }
}