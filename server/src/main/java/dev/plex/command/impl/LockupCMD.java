package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandParameters(name = "lockup", description = "Lockup a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.lockup")
public class LockupCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            return usage();
        }
        Player player = getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = getOfflinePlexPlayer(player.getUniqueId());

        if (isAdmin(getPlexPlayer(player)))
        {
            if (!isConsole(sender))
            {
                assert playerSender != null;
                PlexPlayer plexPlayer1 = getPlexPlayer(playerSender);
                if (!plexPlayer1.getRankFromString().isAtLeast(getPlexPlayer(player).getRankFromString()))
                {
                    return messageComponent("higherRankThanYou");
                }
            }
        }

        punishedPlayer.setLockedUp(!punishedPlayer.isLockedUp());
        if (punishedPlayer.isLockedUp())
        {
            player.openInventory(player.getInventory());
        }
        PlexUtils.broadcast(messageComponent(punishedPlayer.isLockedUp() ? "lockedUpPlayer" : "unlockedPlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckRank(sender, Rank.ADMIN, "plex.lockup") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}