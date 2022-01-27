package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(name = "adventure", aliases = "gma", description = "Set your own or another player's gamemode to adventure mode")
public class AdventureCMD extends PlexCommand
{

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            // doesn't work
            if (sender.isConsoleSender())
            {
                throw new CommandFailException("You must define a player when using the console!");
            }

            sender.getPlayer().setGameMode(GameMode.ADVENTURE);
            send(tl("gameModeSetTo", "adventure"));
            return;
        }

        if (isAdmin(sender.getPlexPlayer()))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.ADVENTURE);
                }
                send(tl("gameModeSetTo", "adventure"));
                return;
            }

            Player player = getNonNullPlayer(args[0]);
            send(tl("setOtherPlayerGameModeTo", player.getName(), "adventure"));
            // use send
            player.sendMessage(tl("playerSetOtherGameMode", sender.getName(), "adventure"));
            player.setGameMode(GameMode.ADVENTURE);
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        if (isAdmin(sender.getPlexPlayer()))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }
}
