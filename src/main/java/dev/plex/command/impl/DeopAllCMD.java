package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "deopall", description = "Deop everyone on the server", aliases = "deopa")
@CommandPermissions(level = Rank.ADMIN)
public class DeopAllCMD extends PlexCommand
{
    @Override
    public Component execute(CommandSender sender, Player playerSender, String[] args)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.setOp(false);
        }
        PlexUtils.broadcast(tl("deoppedAllPlayers", sender.getName()));
        return null;
    }

}