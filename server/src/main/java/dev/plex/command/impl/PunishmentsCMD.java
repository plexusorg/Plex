package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.impl.PunishedPlayerMenu;
import dev.plex.menu.impl.PunishmentMenu;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandParameters(name = "punishments", usage = "/<command> [player]", description = "Opens the Punishments GUI", aliases = "punishlist,punishes")
@CommandPermissions(permission = "plex.punishments", source = RequiredCommandSource.IN_GAME)
public class PunishmentsCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            new PunishmentMenu().open(playerSender);
        }
        else
        {
            if (!DataUtils.hasPlayedBefore(args[0]))
            {
                throw new PlayerNotFoundException();
            }

            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            final PlexPlayer player = offlinePlayer.isOnline() ? getOnlinePlexPlayer(args[0]) : getOfflinePlexPlayer(offlinePlayer.getUniqueId());

            new PunishedPlayerMenu(player).open(playerSender);
        }

        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
