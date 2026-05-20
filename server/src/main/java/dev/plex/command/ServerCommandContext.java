package dev.plex.command;

import com.mojang.brigadier.context.CommandContext;
import dev.plex.Plex;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Runtime context and helper facade for a server command execution.
 */
public final class ServerCommandContext
{
    private final Plex plugin;
    private final PlexCommand command;
    private final CommandContext<CommandSourceStack> brigadierContext;
    private final CommandSender sender;
    private final Player player;
    private final String[] args;

    ServerCommandContext(Plex plugin, PlexCommand command, CommandContext<CommandSourceStack> brigadierContext, String[] args)
    {
        this.plugin = plugin;
        this.command = command;
        this.brigadierContext = brigadierContext;
        this.sender = brigadierContext.getSource().getSender();
        this.player = sender instanceof Player playerSender ? playerSender : null;
        this.args = args;
    }

    /**
     * Returns the running Plex plugin.
     *
     * @return running Plex plugin
     */
    public Plex plugin()
    {
        return plugin;
    }

    /**
     * Returns the Plex command being executed.
     *
     * @return Plex command
     */
    public PlexCommand command()
    {
        return command;
    }

    /**
     * Returns the Brigadier command context.
     *
     * @return Brigadier command context
     */
    public CommandContext<CommandSourceStack> brigadierContext()
    {
        return brigadierContext;
    }

    /**
     * Returns the command sender.
     *
     * @return command sender
     */
    public CommandSender sender()
    {
        return sender;
    }

    /**
     * Returns the player sender, if this command was run by a player.
     *
     * @return player sender, or {@code null} for non-player senders
     */
    public @Nullable Player player()
    {
        return player;
    }

    /**
     * Returns the string-array arguments built from the Brigadier parse.
     *
     * @return execution arguments
     */
    public String[] args()
    {
        return args;
    }

    public PlexPlayer getPlexPlayer(@NotNull Player player)
    {
        return plugin.getPlayerService().getPlayer(player.getUniqueId());
    }

    public void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    public void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    public boolean checkPermission(CommandSender sender, String permission)
    {
        if (!isConsole(sender))
        {
            return checkPermission((Player)sender, permission);
        }
        return true;
    }

    public boolean silentCheckPermission(CommandSender sender, String permission)
    {
        PlexLog.debug("Checking {0} with {1}", sender.getName(), permission);
        if (!isConsole(sender))
        {
            return silentCheckPermission((Player)sender, permission);
        }
        return true;
    }

    public boolean checkPermission(Player player, String permission)
    {
        if (!permission.isEmpty() && !player.hasPermission(permission))
        {
            throw new CommandFailException(PlexUtils.messageString("noPermissionNode", permission));
        }
        return true;
    }

    public boolean silentCheckPermission(Player player, String permission)
    {
        return permission.isEmpty() || player.hasPermission(permission);
    }

    public @Nullable UUID getUUID(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return null;
        }
        return player.getUniqueId();
    }

    public boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    public boolean isConsole()
    {
        return isConsole(sender);
    }

    public Component messageComponent(String s, Object... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    public Component messageComponent(String s, Component... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    public String messageString(String s, Object... objects)
    {
        return PlexUtils.messageString(s, objects);
    }

    public Component usage()
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(command.getUsage()).color(NamedTextColor.GRAY));
    }

    public Component usage(String s)
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(s).color(NamedTextColor.GRAY));
    }

    public Player getNonNullPlayer(String name)
    {
        try
        {
            UUID uuid = UUID.fromString(name);
            return Bukkit.getPlayer(uuid);
        }
        catch (IllegalArgumentException ignored)
        {
        }

        Player player = Bukkit.getPlayer(name);
        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        return player;
    }

    public PlexPlayer getOnlinePlexPlayer(String name)
    {
        Player player = getNonNullPlayer(name);
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(player.getUniqueId());
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    public PlexPlayer getOfflinePlexPlayer(UUID uuid)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(uuid);
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    public World getNonNullWorld(String name)
    {
        World world = Bukkit.getWorld(name);
        if (world == null)
        {
            throw new CommandFailException(PlexUtils.messageString("worldNotFound"));
        }
        return world;
    }

    public Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    public Component noColorComponentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    public Component mmString(String s)
    {
        return PlexUtils.mmDeserialize(s);
    }

    Component exceptionComponent(RuntimeException ex)
    {
        if (ex instanceof PlayerNotFoundException && "PlayerNotFoundException".equals(ex.getMessage()))
        {
            return messageComponent("playerNotFound");
        }
        if (ex instanceof PlayerNotBannedException && "PlayerNotBannedException".equals(ex.getMessage()))
        {
            return messageComponent("playerNotBanned");
        }
        if (ex instanceof ConsoleOnlyException && "ConsoleOnlyException".equals(ex.getMessage()))
        {
            return messageComponent("consoleOnly");
        }
        if (ex instanceof ConsoleMustDefinePlayerException && "ConsoleMustDefinePlayerException".equals(ex.getMessage()))
        {
            return messageComponent("consoleMustDefinePlayer");
        }
        String message = ex.getMessage();
        return message == null ? componentFromString(ex.getClass().getSimpleName()) : PlexUtils.mmDeserialize(message);
    }
}
