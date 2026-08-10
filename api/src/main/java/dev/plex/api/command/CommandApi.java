package dev.plex.api.command;

import dev.plex.command.PlexCommand;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

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

    /**
     * Dispatches a console-capable command with a human-readable audit identity.
     * The command must be invoked from the server's global command thread.
     *
     * @param identityId UUID exposed as the command actor
     * @param identityName name exposed by Plex command contexts
     * @param command command line without a leading slash
     * @param feedback receiver for command feedback
     * @return whether the command was accepted by the dispatcher
     */
    boolean dispatchAsConsole(UUID identityId, String identityName, String command, Consumer<? super Component> feedback);
}
