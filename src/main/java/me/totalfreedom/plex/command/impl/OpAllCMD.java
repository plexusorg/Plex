package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

import static me.totalfreedom.plex.util.PlexUtils.tl;

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
            player.setOp(true);
        PlexUtils.broadcast(tl("oppedAllPlayers", sender.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return ImmutableList.of();
    }
}