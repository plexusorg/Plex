package dev.plex.command.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.adminchat", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adminchat", description = "Talk privately with other admins", usage = "/<command> <message>", aliases = "o,ac,sc,staffchat")
public class AdminChatCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        adminChat(sender, StringUtils.join(args, " "));
        return null;
    }

    private void adminChat(CommandSender sender, String message)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (plugin.getSystem().equalsIgnoreCase("ranks"))
            {
                PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(player.getUniqueId());
                if (plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN) && plexPlayer.isAdminActive())
                {
                    player.sendMessage(PlexUtils.messageComponent("adminChatFormat", sender.getName(), message));
                }
            }
            else if (plugin.getSystem().equalsIgnoreCase("permissions"))
            {
                if (plugin.getPermissionHandler().hasPermission(player, "plex.adminchat"))
                {
                    player.sendMessage(PlexUtils.messageComponent("adminChatFormat", sender.getName(), message));
                }
            }
        }
        plugin.getServer().getConsoleSender().sendMessage(PlexUtils.messageComponent("adminChatFormat", sender.getName(), message));
    }
}
