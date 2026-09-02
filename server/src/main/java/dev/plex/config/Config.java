package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;

import java.io.File;

import org.bukkit.configuration.file.YamlConfiguration;

public class Config extends YamlConfiguration
{
    private final Plex plugin;
    private final File file;
    private final String name;
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
            throw new IllegalStateException("Could not load configuration " + name, ex);
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
            throw new IllegalStateException("Could not save configuration " + name, ex);
        }
    }

    private void saveDefault()
    {
        plugin.saveResource(name, false);
    }
}
