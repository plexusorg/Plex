package dev.plex.api.impl;

import dev.plex.api.config.ModuleConfiguration;
import dev.plex.config.ConfigDefaultsMerger;
import dev.plex.module.PlexModule;
import dev.plex.util.PlexLog;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import org.bukkit.configuration.InvalidConfigurationException;

final class ServerModuleConfiguration extends ModuleConfiguration
{
    private final PlexModule module;
    private final File file;
    private final String from;
    private final String to;

    ServerModuleConfiguration(PlexModule module, String from, String to)
    {
        this.module = module;
        this.file = new File(module.getDataFolder(), to);
        this.from = from;
        this.to = to;
        if (!file.exists()) saveDefault();
    }

    @Override
    public void load()
    {
        try
        {
            ConfigDefaultsMerger.Result result = ConfigDefaultsMerger.merge(file, module.getResource(from), to);
            if (!result.addedKeys().isEmpty())
            {
                PlexLog.log("Merged default key(s) into " + to + ": " + String.join(", ", result.addedKeys()));
            }
            options().parseComments(true);
            super.load(file);
        }
        catch (IOException | InvalidConfigurationException ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void save()
    {
        try { super.save(file); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void saveDefault()
    {
        try
        {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            try (InputStream stream = module.getResource(from))
            {
                if (stream == null)
                {
                    PlexLog.warn("Unable to save default module config " + to + ": missing resource " + from);
                    return;
                }
                Files.copy(stream, file.toPath());
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }
}
