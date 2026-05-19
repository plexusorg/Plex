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
    private final CommandApi commands;
    private final ListenerApi listeners;
    private final ModuleConfigApi moduleConfigs;
    private final LoggingApi logging;
    private final MessageApi messages;
    private final PlayersApi players;
    private final PunishmentsApi punishments;
    private final RollbackApi rollback;
    private final SchedulerApi scheduler;
    private final StorageApi storage;

    public DefaultPlexApi(Plex plugin, int apiCompatibilityVersion)
    {
        this.compatibility = new DefaultApiCompatibility(apiCompatibilityVersion);
        this.configuration = new DefaultConfigurationApi(plugin);
        this.modules = new DefaultModulesApi(plugin);
        this.commands = new DefaultCommandApi(plugin);
        this.listeners = new DefaultListenerApi(plugin);
        this.moduleConfigs = new DefaultModuleConfigApi();
        this.logging = new DefaultLoggingApi();
        this.messages = new DefaultMessageApi();
        this.players = new DefaultPlayersApi(plugin);
        this.punishments = new DefaultPunishmentsApi(plugin);
        this.rollback = new DefaultRollbackApi(plugin);
        this.scheduler = new DefaultSchedulerApi(plugin);
        this.storage = new DefaultStorageApi(plugin);
    }

    @Override public ApiCompatibility compatibility() { return compatibility; }
    @Override public ConfigurationApi configuration() { return configuration; }
    @Override public ModulesApi modules() { return modules; }
    @Override public CommandApi commands() { return commands; }
    @Override public ListenerApi listeners() { return listeners; }
    @Override public ModuleConfigApi moduleConfigs() { return moduleConfigs; }
    @Override public LoggingApi logging() { return logging; }
    @Override public MessageApi messages() { return messages; }
    @Override public PlayersApi players() { return players; }
    @Override public PunishmentsApi punishments() { return punishments; }
    @Override public RollbackApi rollback() { return rollback; }
    @Override public SchedulerApi scheduler() { return scheduler; }
    @Override public StorageApi storage() { return storage; }
}
