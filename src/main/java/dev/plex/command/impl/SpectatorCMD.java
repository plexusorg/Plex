package dev.plex.command.impl;

import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import java.util.List;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "gmsp", description = "Set your own or another player's gamemode to spectator mode")
public class SpectatorCMD extends PlexCommand
{
    public SpectatorCMD()
    {
        super("spectator");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length == 0)
        {
            // doesn't work
            if (sender.isConsoleSender())
            {
                throw new CommandFailException("You must define a player when using the console!");
            }

            sender.getPlayer().setGameMode(GameMode.SPECTATOR);
            send(tl("gameModeSetTo", "spectator"));
            return;
        }

        if (args[0].equals("-a"))
        {
            for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
            {
                targetPlayer.setGameMode(GameMode.SPECTATOR);
            }
            send(tl("gameModeSetTo", "spectator"));
            return;
        }

        Player player = getNonNullPlayer(args[0]);
        send(tl("setOtherPlayerGameModeTo", player.getName(), "spectator"));
        player.sendMessage(tl("playerSetOtherGameMode", sender.getName(), "spectator"));
        player.setGameMode(GameMode.SPECTATOR);
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        if (isAdmin(sender.getPlexPlayer()))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }
}