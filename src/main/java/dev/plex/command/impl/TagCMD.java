package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO: Redo Messages and have customizable messages
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
                return tl("noPermissionConsole");
            }
            PlexPlayer player = DataUtils.getPlayer(playerSender.getUniqueId());
            if (args.length < 2)
            {
                return usage("/tag set <prefix>");
            }
            String prefix = StringUtils.join(args, " ", 1, args.length);
            if (ChatColor.stripColor(prefix).length() > plugin.config.getInt("chat.max-tag-length", 16))
            {
                return componentFromString("The maximum length for a tag may only be " + plugin.config.getInt("chat.max-tag-length", 16));
            }
            player.setPrefix(prefix);
            return Component.text("Your prefix has been set to ").color(NamedTextColor.AQUA).append(componentFromString(prefix));
        }

        if (args[0].equalsIgnoreCase("clear"))
        {
            if (args.length == 1)
            {
                if (sender instanceof ConsoleCommandSender)
                {
                    return tl("noPermissionConsole");
                }

                PlexPlayer player = DataUtils.getPlayer(playerSender.getUniqueId());
                player.setPrefix("");
                return Component.text("Your prefix has been cleared.").color(NamedTextColor.AQUA);
            }

            checkRank(sender, Rank.ADMIN, "plex.tag.clear.others");

            Player target = getNonNullPlayer(args[1]);
            PlexPlayer plexTarget = DataUtils.getPlayer(target.getUniqueId());
            plexTarget.setPrefix("");
            return Component.text("You have cleared " + target.getName() + "'s prefix.").color(NamedTextColor.AQUA);
        }
        return usage();
    }
}
