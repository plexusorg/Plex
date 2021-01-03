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

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "gmc", description = "Set your own or another player's gamemode to creative mode")
public class CreativeCMD extends PlexCommand
{
    public CreativeCMD()
    {
        super("creative");
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

            sender.getPlayer().setGameMode(GameMode.CREATIVE);
            send(tl("gameModeSetTo", "creative"));
            return;
        }

        if (isAdmin(sender.getPlexPlayer()))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.CREATIVE);
                }
                send(tl("gameModeSetTo", "creative"));
                return;
            }

            Player player = getNonNullPlayer(args[0]);
            send(tl("setOtherPlayerGameModeTo", player.getName(), "creative"));
            player.sendMessage(tl("playerSetOtherGameMode", sender.getName(), "creative"));
            player.setGameMode(GameMode.CREATIVE);
        }
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
