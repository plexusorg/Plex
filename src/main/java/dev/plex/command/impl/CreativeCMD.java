package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.gamemode.creative", source = RequiredCommandSource.ANY)
@CommandParameters(name = "creative", aliases = "gmc", description = "Set your own or another player's gamemode to creative mode")
public class CreativeCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException("You must define a player when using the console!");
            }
            playerSender.setGameMode(GameMode.CREATIVE);
            return tl("gameModeSetTo", "creative");
        }

        if (checkRank(playerSender, Rank.ADMIN, "plex.gamemode.creative.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.CREATIVE);
                }
                return tl("gameModeSetTo", "creative");
            }

            Player nPlayer = getNonNullPlayer(args[0]);
            // use send
            send(nPlayer, tl("playerSetOtherGameMode", sender.getName(), "creative"));
            nPlayer.setGameMode(GameMode.CREATIVE);
            return tl("setOtherPlayerGameModeTo", nPlayer.getName(), "creative");
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (isAdmin(sender))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }
}
