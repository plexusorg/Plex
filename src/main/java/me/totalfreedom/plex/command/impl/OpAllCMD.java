package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotations.CommandParameters;
import me.totalfreedom.plex.command.annotations.CommandPermissions;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
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
    public void execute(CommandSender sender, String[] args)
    {
        for (Player player : Bukkit.getOnlinePlayers())
            player.setOp(true);
        PlexUtils.broadcast(tl("oppedAllPlayers", sender.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}