package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.CommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(description = "Op everyone on the server", aliases = "opa")
@CommandPermissions(level = Rank.ADMIN)
public class OpAllCMD extends PlexCommand
{
    public OpAllCMD()
    {
        super("opall");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.setOp(true);
        }
        PlexUtils.broadcast(tl("oppedAllPlayers", sender.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return ImmutableList.of();
    }
}