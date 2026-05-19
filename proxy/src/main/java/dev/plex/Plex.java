package dev.plex;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.plex.api.PlexApi;
import dev.plex.api.impl.DefaultPlexApi;
import dev.plex.config.YamlConfig;
import dev.plex.handlers.ListenerHandler;
import dev.plex.util.PlexLog;
import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;
import lombok.Getter;

@Plugin(
        name = "Plex",
        id = "plex",
        version = BuildParameters.VERSION,
        url = "https://plex.us.org",
        description = "Plex provides a new experience for freedom servers.",
        authors = {"Telesphoreo", "Taah"}
)
@Getter
public class Plex
{
    public static final int MODULE_API_COMPATIBILITY_VERSION = 1;
    private static Plex plugin;

    public final ProxyServer server;
    private final Logger logger;
    private final File dataFolder;

    private YamlConfig config;
    private YamlConfig messages;
    private PlexApi api;

    @Inject
    public Plex(ProxyServer server, Logger logger, @DataDirectory Path folder)
    {
        plugin = this;
        this.server = server;
        this.logger = logger;
        this.dataFolder = folder.toFile();
        if (!dataFolder.exists())
        {
            dataFolder.mkdir();
        }
        PlexLog.log("Enabling Plex-Velocity");
    }

    @Subscribe
    public void onStart(ProxyInitializeEvent event)
    {
        this.config = loadConfig("config.yml");
        this.messages = loadConfig("messages.yml");
        this.api = new DefaultPlexApi(this, MODULE_API_COMPATIBILITY_VERSION);
        new ListenerHandler();
    }

    private YamlConfig loadConfig(String name)
    {
        YamlConfig yamlConfig = new YamlConfig(dataFolder, name);
        if (yamlConfig.create())
        {
            PlexLog.log("Created configuration '" + name + "'");
        }
        PlexLog.log("Loaded configuration '" + name + "'");
        return yamlConfig;
    }

    public static Plex get()
    {
        return plugin;
    }
}
