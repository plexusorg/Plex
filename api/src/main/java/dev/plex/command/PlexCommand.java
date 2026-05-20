package dev.plex.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.command.source.RequiredCommandSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;

/**
 * Public Brigadier command contract for Plex and Plex modules.
 */
public interface PlexCommand
{
    /**
     * Returns explicit command metadata.
     *
     * @return command metadata
     */
    CommandSpec commandSpec();

    /**
     * Builds the Brigadier command tree for this command.
     *
     * @return root literal command node
     */
    LiteralCommandNode<CommandSourceStack> buildCommand();

    /**
     * Returns the primary command name.
     *
     * @return primary command name
     */
    default String getName()
    {
        return commandSpec().name();
    }

    /**
     * Returns the command description.
     *
     * @return command description
     */
    default String getDescription()
    {
        return commandSpec().description();
    }

    /**
     * Returns command usage text.
     *
     * @return command usage text with {@code <command>} replaced by the command name
     */
    default String getUsage()
    {
        return commandSpec().resolvedUsage();
    }

    /**
     * Returns the permission node required to use the command.
     *
     * @return permission node required to use the command
     */
    default String getPermission()
    {
        return commandSpec().permission();
    }

    /**
     * Returns the command source required to run the command.
     *
     * @return command source required to run the command
     */
    default RequiredCommandSource getRequiredSource()
    {
        return commandSpec().requiredSource();
    }

    /**
     * Returns command aliases as a trimmed list.
     *
     * @return command aliases
     */
    default List<String> getAliases()
    {
        return commandSpec().aliases();
    }
}
