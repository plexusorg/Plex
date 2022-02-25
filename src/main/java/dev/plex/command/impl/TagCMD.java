package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.tag", source = RequiredCommandSource.ANY)
@CommandParameters(name = "tag", aliases = "prefix", description = "Manages your prefix", usage = "/<command> <set | clear> <prefix | player>")
public class TagCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (sender instanceof ConsoleCommandSender)
            {
                return usage("/tag clear <player>");
            }
            return usage();
        }

        if (args[0].equalsIgnoreCase("set"))
        {
            if (sender instanceof ConsoleCommandSender)
            {
                return messageComponent("noPermissionConsole");
            }
            PlexPlayer player = DataUtils.getPlayer(playerSender.getUniqueId());
            if (args.length < 2)
            {
                return usage("/tag set <prefix>");
            }
            String prefix = StringUtils.join(args, " ", 1, args.length);
            if (ChatColor.stripColor(prefix).length() > plugin.config.getInt("chat.max-tag-length", 16))
            {
                return messageComponent("maximumPrefixLength", plugin.config.getInt("chat.max-tag-length", 16));
            }
            player.setPrefix(prefix);
            return messageComponent("prefixSetTo", prefix);
        }

        if (args[0].equalsIgnoreCase("clear"))
        {
            if (args.length == 1)
            {
                if (sender instanceof ConsoleCommandSender)
                {
                    return messageComponent("noPermissionConsole");
                }

                PlexPlayer player = DataUtils.getPlayer(playerSender.getUniqueId());
                player.setPrefix("");
                return messageComponent("prefixCleared");
            }

            checkRank(sender, Rank.ADMIN, "plex.tag.clear.others");
            Player target = getNonNullPlayer(args[1]);
            PlexPlayer plexTarget = DataUtils.getPlayer(target.getUniqueId());
            plexTarget.setPrefix("");
            messageComponent("otherPrefixCleared");
        }
        return usage();
    }
}
