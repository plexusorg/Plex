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

@CommandParameters(name = "list", description = "Freeze a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.OP)
public class ListCMD extends PlexCommand
{
    @Override
    protected Component execute(CommandSender sender, String[] args)
    {
        List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());
        Component component = Component.text("There " + (players.size() > 1 ? "are" : "is") + " currently").color(NamedTextColor.GRAY)
                .append(Component.space())
                .append(Component.text(players.size()).color(NamedTextColor.YELLOW))
                .append(Component.space())
                .append(Component.text(players.size() > 1 ? "players " : "player " + "online out of").color(NamedTextColor.GRAY))
                .append(Component.space())
                .append(Component.text(Bukkit.getMaxPlayers()).color(NamedTextColor.YELLOW))
                .append(Component.newline());
        for (int i = 0; i < players.size(); i++)
        {
            Player player = players.get(i);
            component = component.append(Component.text(getPlexPlayer(player).getRankFromString().getPrefix())).append(Component.space()).append(Component.text(player.getName()).color(NamedTextColor.WHITE));
            if (i != players.size() - 1)
            {
                component = component.append(Component.text(",")).append(Component.space());
            }
        }

        return component;
    }
}
