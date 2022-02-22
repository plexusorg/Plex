package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandParameters(name = "unban", usage = "/<command> <player>", description = "Unbans a player, offline or online")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.ban", source = RequiredCommandSource.ANY)

public class UnbanCMD extends PlexCommand
{
    @Override
    public Component execute(@NotNull CommandSender sender, @NotNull Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args.length == 1)
        {
            UUID targetUUID = PlexUtils.getFromName(args[0]);
            PlexPlayer plexPlayer = getOfflinePlexPlayer(targetUUID);

            if (!DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }

            if (!plugin.getPunishmentManager().isBanned(targetUUID))
            {
                throw new PlayerNotBannedException();
            }

            plugin.getPunishmentManager().unban(targetUUID);
            PlexUtils.broadcast(tl("unbanningPlayer", sender.getName(), plexPlayer.getName()));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && checkTab(sender, Rank.ADMIN, "plex.unban") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
