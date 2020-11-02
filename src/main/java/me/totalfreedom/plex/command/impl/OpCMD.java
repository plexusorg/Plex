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

@CommandParameters(description = "Op a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.OP)
public class OpCMD extends PlexCommand
{
    public OpCMD()
    {
        super("op");
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        Player player = Bukkit.getPlayer(args[0]);
        if (player == null)
        {
            sender.sendMessage(tl("playerNotFound"));
            return;
        }
        PlexUtils.broadcast(tl("oppedPlayer", sender.getName(), player.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}