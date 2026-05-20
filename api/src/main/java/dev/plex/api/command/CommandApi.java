package dev.plex.api.command;

import dev.plex.command.PlexCommand;
import java.util.List;

/**
 * Registers and unregisters Plex commands with the running platform.
 *
 * <p>Commands are installed through Paper's Brigadier command lifecycle. A command
 * registered before that lifecycle event is active in the current server command
 * tree. A command registered or unregistered after that lifecycle event is staged
 * in Plex's registry and takes effect the next time Paper rebuilds lifecycle
 * commands, such as on a full server restart.</p>
 */
public interface CommandApi
{
    /**
     * Registers a command with Plex.
     *
     * @param command command to register
     */
    void register(PlexCommand command);

    /**
     * Unregisters a command from Plex.
     *
     * <p>If Paper's Brigadier lifecycle has already registered commands for this
     * server run, the command may remain in the active dispatcher until Paper
     * rebuilds lifecycle commands.</p>
     *
     * @param command command to unregister
     */
    void unregister(PlexCommand command);

    /**
     * Returns the commands currently tracked by Plex.
     *
     * @return registered commands
     */
    List<PlexCommand> registeredCommands();

    /**
     * Returns whether command changes are staged for the next Paper command
     * lifecycle rebuild.
     *
     * @return {@code true} when command registration or unregistration changed
     *         after the active command lifecycle was built
     */
    boolean requiresLifecycleReload();
}
