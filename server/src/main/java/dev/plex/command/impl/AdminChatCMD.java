package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import dev.plex.util.redis.MessageUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@CommandPermissions(permission = "plex.adminchat", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adminchat", description = "Talk privately with other admins", usage = "/<command> <message>", aliases = "o,ac,sc,staffchat")
public class AdminChatCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        PlexPlayer player;
        if (args.length == 0)
        {
            if (playerSender != null)
            {
                player = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
                player.setStaffChat(!player.isStaffChat());
                return messageComponent("adminChatToggled", BooleanUtils.toStringOnOff(player.isStaffChat()));
            }
            return usage();
        }

        String prefix;
        if (playerSender != null)
        {
            player = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            prefix = player.getPrefix();
        }
        else
        {
            prefix = messageString(LegacyComponentSerializer.legacyAmpersand().serialize(mmString("<dark_gray>[<dark_purple>Console<dark_gray]")));
        }
        String message = StringUtils.join(args, " ");
        plugin.getServer().getConsoleSender().sendMessage(messageComponent("adminChatFormat", sender.getName(), prefix, message));
        MessageUtil.sendStaffChat(sender, SafeMiniMessage.mmDeserialize(message), PlexUtils.adminChat(sender.getName(), prefix, message).toArray(UUID[]::new));
        return null;
    }
}
