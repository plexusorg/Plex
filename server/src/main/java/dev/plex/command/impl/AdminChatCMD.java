package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.event.StaffChatMessageEvent;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import dev.plex.util.redis.MessageUtil;

import java.util.UUID;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminChatCMD extends ServerCommand
{
    public AdminChatCMD()
    {
        super(command("adminchat")
            .description("Talk privately with other admins")
            .usage("/<command> <message>")
            .aliases("o,sc,staffchat")
            .permission("plex.adminchat")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(greedyString("message")
                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "message")))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        PlexPlayer player;
        if (args.length == 0)
        {
            if (playerSender != null)
            {
                player = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
                player.setStaffChat(!player.isStaffChat());
                return context.messageComponent("adminChatToggled", context.messageString(player.isStaffChat() ? "stateOn" : "stateOff"));
            }
            return context.usage();
        }

        String prefix;
        if (playerSender != null)
        {
            player = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            prefix = PlexUtils.mmSerialize(VaultHook.getPrefix(player));
        }
        else
        {
            prefix = "<dark_gray>[<dark_purple>Console<dark_gray>]";
        }
        PlexLog.debug("admin chat prefix: {0}", prefix);
        String message = StringUtils.join(args, " ");
        StaffChatMessageEvent staffChatEvent = new StaffChatMessageEvent(
                sender,
                SafeMiniMessage.mmDeserialize(message),
                StaffChatMessageEvent.Source.COMMAND,
                !Bukkit.isPrimaryThread());
        plugin.getServer().getPluginManager().callEvent(staffChatEvent);
        if (staffChatEvent.isCancelled())
        {
            return null;
        }
        Component eventMessage = staffChatEvent.getMessage();
        String serializedMessage = SafeMiniMessage.mmSerialize(eventMessage);
        plugin.getServer().getConsoleSender().sendMessage(context.messageComponent("adminChatFormat", sender.getName(), prefix, serializedMessage));
        MessageUtil.sendStaffChat(plugin, sender, eventMessage, PlexUtils.adminChat(sender.getName(), prefix, serializedMessage).toArray(UUID[]::new));
        return null;
    }

}
