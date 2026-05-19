package dev.plex.api;

import dev.plex.api.command.CommandApi;
import dev.plex.api.config.ConfigurationApi;
import dev.plex.api.config.ModuleConfigApi;
import dev.plex.api.listener.ListenerApi;
import dev.plex.api.logging.LoggingApi;
import dev.plex.api.message.MessageApi;
import dev.plex.api.module.ModulesApi;
import dev.plex.api.player.PlayersApi;
import dev.plex.api.punishment.PunishmentsApi;
import dev.plex.api.scheduler.SchedulerApi;
import dev.plex.api.storage.StorageApi;

/**
 * Public API facade exposed to Plex modules.
 *
 * <p>Keep this interface small and deliberate; adding a method here makes it
 * part of the supported module API contract.</p>
 */
public interface PlexApi
{
    /**
     * @return module API compatibility information for this Plex build
     */
    ApiCompatibility compatibility();

    /**
     * @return safe access to shared Plex configuration files
     */
    ConfigurationApi configuration();

    /**
     * @return safe access to module metadata and module-related operations
     */
    ModulesApi modules();

    CommandApi commands();

    ListenerApi listeners();

    ModuleConfigApi moduleConfigs();

    LoggingApi logging();

    MessageApi messages();

    PlayersApi players();

    PunishmentsApi punishments();

    SchedulerApi scheduler();

    StorageApi storage();
}
