package dev.plex.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.event.StaffChatMessageEvent;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
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
        command.executes(context -> executeCommand(context, this::toggle));
        command.then(greedyString("message")
                .executes(context -> executeCommand(context, commandContext ->
                        sendMessage(commandContext, normalizeGreedyString(string(context, "message"))))));
    }

    private Component toggle(ServerCommandContext context)
    {
        Player playerSender = context.player();
        if (playerSender != null)
        {
            PlexPlayer player = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
            player.setStaffChat(!player.isStaffChat());
            return PlexUtils.messageComponent("adminChatToggled", Placeholder.parsed("state", PlexUtils.messageString(player.isStaffChat() ? "stateOn" : "stateOff")));
        }
        return context.usage();
    }

    private Component sendMessage(ServerCommandContext context, String message)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        Component prefix;
        if (playerSender != null)
        {
            PlexPlayer player = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
            prefix = VaultHook.getPrefix(player);
        }
        else
        {
            prefix = PlexUtils.mmDeserialize("<dark_gray>[<dark_purple>Console<dark_gray>]");
        }
        PlexLog.debug("admin chat prefix: {0}", PlexUtils.mmSerialize(prefix));
        StaffChatMessageEvent staffChatEvent = new StaffChatMessageEvent(
                sender,
                PlexUtils.stringToComponent(message),
                StaffChatMessageEvent.Source.COMMAND,
                !Bukkit.isPrimaryThread());
        plugin.getServer().getPluginManager().callEvent(staffChatEvent);
        if (staffChatEvent.isCancelled())
        {
            return null;
        }
        Component eventMessage = staffChatEvent.getMessage();
        plugin.getServer().getConsoleSender().sendMessage(PlexUtils.messageComponent("adminChatFormat", Placeholder.unparsed("sender", context.senderName()), Placeholder.component("prefix", prefix), Placeholder.component("message", eventMessage)));
        MessageUtil.sendStaffChat(plugin, sender, eventMessage, PlexUtils.adminChat(context.senderName(), prefix, eventMessage).toArray(UUID[]::new));
        return null;
    }

}
