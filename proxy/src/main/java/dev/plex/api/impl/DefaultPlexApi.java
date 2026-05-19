package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.ApiCompatibility;
import dev.plex.api.PlexApi;
import dev.plex.api.command.CommandApi;
import dev.plex.api.config.ConfigurationApi;
import dev.plex.api.config.ModuleConfigApi;
import dev.plex.api.listener.ListenerApi;
import dev.plex.api.logging.LoggingApi;
import dev.plex.api.message.MessageApi;
import dev.plex.api.module.ModulesApi;
import dev.plex.api.player.PlayersApi;
import dev.plex.api.punishment.PunishmentsApi;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.api.scheduler.SchedulerApi;
import dev.plex.api.storage.StorageApi;

public final class DefaultPlexApi implements PlexApi
{
    private final ApiCompatibility compatibility;
    private final ConfigurationApi configuration;
    private final ModulesApi modules;

    public DefaultPlexApi(Plex plugin, int apiCompatibilityVersion)
    {
        this.compatibility = new DefaultApiCompatibility(apiCompatibilityVersion);
        this.configuration = new DefaultConfigurationApi(plugin);
        this.modules = new DefaultModulesApi();
    }

    @Override public ApiCompatibility compatibility() { return compatibility; }
    @Override public ConfigurationApi configuration() { return configuration; }
    @Override public ModulesApi modules() { return modules; }
    @Override public CommandApi commands() { throw unsupported(); }
    @Override public ListenerApi listeners() { throw unsupported(); }
    @Override public ModuleConfigApi moduleConfigs() { throw unsupported(); }
    @Override public LoggingApi logging() { throw unsupported(); }
    @Override public MessageApi messages() { throw unsupported(); }
    @Override public PlayersApi players() { throw unsupported(); }
    @Override public PunishmentsApi punishments() { throw unsupported(); }
    @Override public RollbackApi rollback() { throw unsupported(); }
    @Override public SchedulerApi scheduler() { throw unsupported(); }
    @Override public StorageApi storage() { throw unsupported(); }

    private static UnsupportedOperationException unsupported()
    {
        return new UnsupportedOperationException("This Plex API service is only available on the server platform");
    }
}
