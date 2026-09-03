package dev.plex.command;

import com.mojang.brigadier.context.CommandContext;
import dev.plex.api.command.CommandExecutionIdentity;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Context for a server command execution.
 */
public final class ServerCommandContext
{
    private final String commandUsage;
    private final CommandSender sender;
    private final String senderName;
    private final UUID senderUuid;
    private final Player player;

    ServerCommandContext(PlexCommand command, CommandContext<CommandSourceStack> brigadierContext)
    {
        this.commandUsage = command.getUsage();
        this.sender = brigadierContext.getSource().getSender();
        this.senderName = CommandExecutionIdentity.currentName(sender.getName());
        this.senderUuid = sender instanceof Player playerSender
                ? playerSender.getUniqueId()
                : CommandExecutionIdentity.currentUniqueId();
        this.player = sender instanceof Player playerSender ? playerSender : null;
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
     * Returns the attributed sender name for messages and audit logs.
     *
     * @return attributed sender name
     */
    public String senderName()
    {
        return senderName;
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
        PlexLog.debug("Checking {0} with {1}", senderName, permission);
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
        if (sender instanceof Player player)
        {
            return player.getUniqueId();
        }
        return sender == this.sender ? senderUuid : null;
    }

    public boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    public boolean isConsole()
    {
        return isConsole(sender);
    }

    public Component usage()
    {
        return PlexUtils.messageComponent("correctUsagePrefix").append(componentFromString(commandUsage).color(NamedTextColor.GRAY));
    }

    public Component usage(String s)
    {
        return PlexUtils.messageComponent("correctUsagePrefix").append(componentFromString(s).color(NamedTextColor.GRAY));
    }

    private Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    Component exceptionComponent(RuntimeException ex)
    {
        if (ex instanceof PlayerNotFoundException && "PlayerNotFoundException".equals(ex.getMessage()))
        {
            return PlexUtils.messageComponent("playerNotFound");
        }
        if (ex instanceof PlayerNotBannedException && "PlayerNotBannedException".equals(ex.getMessage()))
        {
            return PlexUtils.messageComponent("playerNotBanned");
        }
        if (ex instanceof ConsoleOnlyException && "ConsoleOnlyException".equals(ex.getMessage()))
        {
            return PlexUtils.messageComponent("consoleOnly");
        }
        if (ex instanceof ConsoleMustDefinePlayerException && "ConsoleMustDefinePlayerException".equals(ex.getMessage()))
        {
            return PlexUtils.messageComponent("consoleMustDefinePlayer");
        }
        String message = ex.getMessage();
        return message == null ? componentFromString(ex.getClass().getSimpleName()) : PlexUtils.mmDeserialize(message);
    }
}
