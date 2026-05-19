package dev.plex.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Arrays;
import java.util.List;

/**
 * Public Brigadier command contract for Plex and Plex modules.
 */
public interface PlexCommand
{
    /**
     * Builds the Brigadier command tree for this command.
     *
     * @return root literal command node
     */
    LiteralCommandNode<CommandSourceStack> buildCommand();

    /**
     * Reads command parameter metadata from {@link CommandParameters}.
     *
     * @return command parameter metadata
     * @throws IllegalStateException if the command class is missing {@link CommandParameters}
     */
    default CommandParameters parameters()
    {
        CommandParameters parameters = getClass().getAnnotation(CommandParameters.class);
        if (parameters == null)
        {
            throw new IllegalStateException(getClass().getName() + " requires a CommandParameters annotation");
        }
        return parameters;
    }

    /**
     * Reads command permission metadata from {@link CommandPermissions}.
     *
     * @return command permission metadata
     * @throws IllegalStateException if the command class is missing {@link CommandPermissions}
     */
    default CommandPermissions permissions()
    {
        CommandPermissions permissions = getClass().getAnnotation(CommandPermissions.class);
        if (permissions == null)
        {
            throw new IllegalStateException(getClass().getName() + " requires a CommandPermissions annotation");
        }
        return permissions;
    }

    /**
     * Returns the primary command name.
     *
     * @return primary command name
     */
    default String getName()
    {
        return parameters().name();
    }

    /**
     * Returns the command description.
     *
     * @return command description
     */
    default String getDescription()
    {
        return parameters().description();
    }

    /**
     * Returns command usage text.
     *
     * @return command usage text with {@code <command>} replaced by the command name
     */
    default String getUsage()
    {
        return parameters().usage().replace("<command>", getName());
    }

    /**
     * Returns the permission node required to use the command.
     *
     * @return permission node required to use the command
     */
    default String getPermission()
    {
        return permissions().permission();
    }

    /**
     * Returns the command source required to run the command.
     *
     * @return command source required to run the command
     */
    default RequiredCommandSource getRequiredSource()
    {
        return permissions().source();
    }

    /**
     * Returns command aliases as a trimmed list.
     *
     * @return comma-separated aliases from {@link CommandParameters#aliases()} as a trimmed list
     */
    default List<String> getAliases()
    {
        String aliases = parameters().aliases();
        if (aliases.isBlank())
        {
            return List.of();
        }
        return Arrays.stream(aliases.split(","))
                .map(String::trim)
                .filter(alias -> !alias.isBlank())
                .toList();
    }
}
