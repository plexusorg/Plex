package dev.plex.api.command;

import dev.plex.command.PlexCommand;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;

/**
 * Registers and unregisters Plex commands.
 *
 * <p>Register commands during module load. Paper can then add them to the
 * command list for the current server run. Later changes usually require a
 * server restart.</p>
 *
 * <p>Modules must use the methods on {@link dev.plex.module.PlexModule} so that
 * Plex can unregister their commands during module unload.</p>
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
     * <p>The command can remain active until Paper rebuilds its command list.
     * This usually happens after a server restart.</p>
     *
     * @param command command to unregister
     */
    void unregister(PlexCommand command);

    /**
     * Returns the commands tracked by Plex.
     *
     * @return registered commands
     */
    List<PlexCommand> registeredCommands();

    /**
     * Checks if command changes require Paper to rebuild its command list.
     *
     * @return {@code true} if Paper must rebuild the command list
     */
    boolean requiresLifecycleReload();

    /**
     * Runs a command as the console and records the given actor.
     * Call this method from the global server thread.
     *
     * @param identityId actor UUID
     * @param identityName actor name
     * @param command command line without a leading slash
     * @param feedback receiver for command feedback
     * @return {@code true} if Paper accepted the command
     */
    boolean dispatchAsConsole(UUID identityId, String identityName, String command, Consumer<? super Component> feedback);
}
