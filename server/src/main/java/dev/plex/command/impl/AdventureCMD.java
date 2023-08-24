package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandPermissions(level = Rank.OP, permission = "plex.gamemode.adventure", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adventure", aliases = "gma,egma,eadventure,adventuremode,eadventuremode", description = "Set your own or another player's gamemode to adventure mode")
public class AdventureCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException(messageString("consoleMustDefinePlayer"));
            }
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender, GameMode.ADVENTURE));
            return null;
        }

        if (checkRank(sender, Rank.ADMIN, "plex.gamemode.adventure.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.ADVENTURE);
                    messageComponent("gameModeSetTo", "adventure");
                }
                PlexUtils.broadcast(messageComponent("setEveryoneGameMode", sender.getName(), "adventure"));
                return null;
            }

            Player nPlayer = getNonNullPlayer(args[0]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, nPlayer, GameMode.ADVENTURE));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (silentCheckRank(sender, Rank.ADMIN, "plex.gamemode.adventure.others"))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }
}
