package dev.plex.api;

import dev.plex.api.command.CommandApi;
import dev.plex.api.config.ConfigurationApi;
import dev.plex.api.config.ModuleConfigApi;
import dev.plex.api.listener.ListenerApi;
import dev.plex.api.logging.LoggingApi;
import dev.plex.api.message.MessageApi;
import dev.plex.api.module.ModulesApi;
import dev.plex.api.note.NotesApi;
import dev.plex.api.player.PlayersApi;
import dev.plex.api.punishment.PunishmentsApi;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.api.scheduler.SchedulerApi;
import dev.plex.api.storage.StorageApi;

/**
 * Gives Plex modules access to supported services.
 */
public interface PlexApi
{
    /**
     * Returns the API version for this Plex build.
     *
     * @return API compatibility version
     */
    int apiCompatibilityVersion();

    /**
     * Returns the shared Plex configuration files.
     *
     * @return shared Plex configuration files
     */
    ConfigurationApi configuration();

    /**
     * Returns information about loaded modules.
     *
     * @return loaded module information
     */
    ModulesApi modules();

    /**
     * Returns the command service.
     *
     * @return command service
     */
    CommandApi commands();

    /**
     * Returns the listener service.
     *
     * @return listener service
     */
    ListenerApi listeners();

    /**
     * Returns the module configuration service.
     *
     * @return module configuration service
     */
    ModuleConfigApi moduleConfigs();

    /**
     * Returns the logging service.
     *
     * @return logging service
     */
    LoggingApi logging();

    /**
     * Returns the message service.
     *
     * @return message service
     */
    MessageApi messages();

    /**
     * Returns the player notes service.
     *
     * @return player notes service
     */
    NotesApi notes();

    /**
     * Returns the player service.
     *
     * @return player service
     */
    PlayersApi players();

    /**
     * Returns the punishment service.
     *
     * @return punishment service
     */
    PunishmentsApi punishments();

    /**
     * Returns the rollback service.
     *
     * @return rollback service
     */
    RollbackApi rollback();

    /**
     * Returns the task scheduler.
     * Modules must use {@link dev.plex.module.PlexModule#scheduler()} so that
     * Plex can cancel their tasks during module unload.
     *
     * @return task scheduler
     */
    SchedulerApi scheduler();

    /**
     * Returns the SQL storage service.
     *
     * @return SQL storage service
     */
    StorageApi storage();
}
