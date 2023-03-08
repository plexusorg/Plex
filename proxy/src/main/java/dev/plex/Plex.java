package dev.plex;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.plex.config.TomlConfig;
import dev.plex.handlers.ListenerHandler;
import dev.plex.settings.ServerSettings;
import dev.plex.util.PlexLog;
import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Credits for TOML library go to https://github.com/mwanji/toml4j
 * I was unable to add it back to the package without it glitching, so
 * I kept it in a separate package.
 * <p>
 * Modifications: Properly indent arrays in TOML as well as only append
 * missing object fields into the file
 */

@Plugin(
        name = "Plex",
        id = "plex",
        version = "@version@",
        url = "https://plex.us.org",
        description = "Plex provides a new experience for freedom servers.",
        authors = {"Telesphoreo", "Taah"}
)
@Getter
public class Plex
{
    private static Plex plugin;

    public final ProxyServer server;
    private final Logger logger;
    private final File dataFolder;

    private TomlConfig config;

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
        this.config = new TomlConfig("config.toml");
        this.config.setOnCreate(toml ->
        {
            PlexLog.log("Created configuration 'config.toml'");
        });
        this.config.setOnLoad(toml ->
        {
            PlexLog.log("Loaded configuration 'config.toml'");
        });
        this.config.create(true);
        this.config.write(new ServerSettings());
        new ListenerHandler();
    }

    public static Plex get()
    {
        return plugin;
    }
}
