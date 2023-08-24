package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.unmute")
@CommandParameters(name = "unmute", description = "Unmute a player", usage = "/<command> <player>")
public class UnmuteCMD extends PlexCommand
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
        if (!punishedPlayer.isMuted())
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotMuted"));
        }
        punishedPlayer.setMuted(false);
        PlexUtils.broadcast(messageComponent("unmutedPlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckRank(sender, Rank.ADMIN, "plex.unfreeze") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
