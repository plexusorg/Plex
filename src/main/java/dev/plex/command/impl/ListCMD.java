package dev.plex.command.impl;

import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "list", description = "Show a list of all online players")
@CommandPermissions(level = Rank.OP, permission = "plex.list")
public class ListCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());
        Component list = Component.empty();
        Component header = Component.text("There " + (players.size() == 1 ? "is" : "are") + " currently").color(NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(players.size()).color(NamedTextColor.YELLOW))
                .append(Component.space())
                .append(Component.text(players.size() == 1 ? "player" : "players").color(NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text("online out of").color(NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(Bukkit.getMaxPlayers()).color(NamedTextColor.YELLOW))
                .append(Component.space())
                .append(Component.text(Bukkit.getMaxPlayers() == 1 ? "player." : "players.").color(NamedTextColor.GRAY));
        send(sender, header);
        for (int i = 0; i < players.size(); i++)
        {
            Player player = players.get(i);
            list = list.append(getPlexPlayer(player).getRankFromString().getPrefix()).append(Component.space()).append(Component.text(player.getName()).color(NamedTextColor.WHITE));
            if (i != players.size() - 1)
            {
                list = list.append(Component.text(",")).append(Component.space());
            }
        }
        return list;
    }
}
