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
    LiteralCommandNode<CommandSourceStack> buildCommand();

    default CommandParameters parameters()
    {
        CommandParameters parameters = getClass().getAnnotation(CommandParameters.class);
        if (parameters == null)
        {
            throw new IllegalStateException(getClass().getName() + " requires a CommandParameters annotation");
        }
        return parameters;
    }

    default CommandPermissions permissions()
    {
        CommandPermissions permissions = getClass().getAnnotation(CommandPermissions.class);
        if (permissions == null)
        {
            throw new IllegalStateException(getClass().getName() + " requires a CommandPermissions annotation");
        }
        return permissions;
    }

    default String getName()
    {
        return parameters().name();
    }

    default String getDescription()
    {
        return parameters().description();
    }

    default String getUsage()
    {
        return parameters().usage().replace("<command>", getName());
    }

    default String getPermission()
    {
        return permissions().permission();
    }

    default RequiredCommandSource getRequiredSource()
    {
        return permissions().source();
    }

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
