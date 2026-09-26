package dev.plex.command;

import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.api.PlexApi;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.PlexModule;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import dev.plex.command.exception.AmbiguousPlayerException;
import dev.plex.command.exception.PlayerNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Public Brigadier command contract for Plex and Plex modules.
 */
public interface PlexCommand
{
    /**
     * Returns the command definition.
     *
     * @return command definition
     */
    CommandSpec commandSpec();

    // Call only on the command thread, not from an asynchronous lookup callback.
    static Player resolveOnlinePlayer(String name)
    {
        UUID uuid = null;
        try
        {
            uuid = UUID.fromString(name);
        }
        catch (IllegalArgumentException ignored)
        {
            // A non-UUID argument is a name.
        }
        Player exact = uuid == null ? Bukkit.getPlayerExact(name) : Bukkit.getPlayer(uuid);
        if (exact != null)
        {
            return exact;
        }
        if (uuid != null)
        {
            throw new PlayerNotFoundException();
        }
        Player match = null;
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player.getName().regionMatches(true, 0, name, 0, name.length()))
            {
                match = player;
                names.add(player.getName());
            }
        }
        if (names.size() > 1)
        {
            names.sort(String.CASE_INSENSITIVE_ORDER);
            throw new AmbiguousPlayerException(names);
        }
        if (match == null)
        {
            throw new PlayerNotFoundException();
        }
        return match;
    }

    /**
     * Builds the Brigadier command tree for this command.
     *
     * @return root literal command node
     */
    LiteralCommandNode<CommandSourceStack> buildCommand();

    /**
     * Supplies the running Plex API to commands that need API helpers.
     *
     * <p>The default implementation does not store the API.</p>
     *
     * @param api running Plex API
     */
    default void bindApi(PlexApi api)
    {
    }

    /**
     * Supplies the owning module to commands that need module-owned resources.
     *
     * @param module owning module
     */
    default void bindModule(PlexModule module)
    {
    }

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
