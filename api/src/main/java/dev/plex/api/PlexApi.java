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
import dev.plex.api.rollback.RollbackApi;
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
     * Returns module API compatibility information for this Plex build.
     *
     * @return module API compatibility information for this Plex build
     */
    ApiCompatibility compatibility();

    /**
     * Returns access to shared Plex configuration files.
     *
     * @return safe access to shared Plex configuration files
     */
    ConfigurationApi configuration();

    /**
     * Returns access to module metadata and module-related operations.
     *
     * @return safe access to module metadata and module-related operations
     */
    ModulesApi modules();

    /**
     * Returns command registration operations.
     *
     * @return command registration operations for Plex commands
     */
    CommandApi commands();

    /**
     * Returns listener registration operations.
     *
     * @return listener registration operations for Bukkit listeners
     */
    ListenerApi listeners();

    /**
     * Returns module configuration creation operations.
     *
     * @return module configuration creation operations
     */
    ModuleConfigApi moduleConfigs();

    /**
     * Returns logging operations.
     *
     * @return Plex logging operations
     */
    LoggingApi logging();

    /**
     * Returns message formatting and broadcast operations.
     *
     * @return message formatting and broadcast operations
     */
    MessageApi messages();

    /**
     * Returns player lookup operations.
     *
     * @return player lookup operations
     */
    PlayersApi players();

    /**
     * Returns punishment lookup and creation operations.
     *
     * @return punishment lookup and creation operations
     */
    PunishmentsApi punishments();

    /**
     * Returns CoreProtect rollback operations.
     *
     * @return CoreProtect rollback operations
     */
    RollbackApi rollback();

    /**
     * Returns Paper and Folia scheduler operations.
     *
     * @return Paper and Folia scheduler operations
     */
    SchedulerApi scheduler();

    /**
     * Returns SQL storage access operations.
     *
     * @return SQL storage access operations
     */
    StorageApi storage();
}
