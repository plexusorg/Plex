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

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.ANY)
@CommandParameters(name = "spectator", aliases = "gmsp", description = "Set your own or another player's gamemode to spectator mode")
public class SpectatorCMD extends PlexCommand
{
    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException("You must define a player when using the console!");
            }
            Player player = (Player) sender;
            player.setGameMode(GameMode.SPECTATOR);
            return tl("gameModeSetTo", "spectator");
        }

        if (isAdmin(sender))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.SPECTATOR);
                }
                return tl("gameModeSetTo", "spectator");
            }

            Player player = getNonNullPlayer(args[0]);
            // use send
            send(player, tl("playerSetOtherGameMode", sender.getName(), "spectator"));
            player.setGameMode(GameMode.SPECTATOR);
            return tl("setOtherPlayerGameModeTo", player.getName(), "spectator");
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