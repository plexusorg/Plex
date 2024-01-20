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
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandParameters(name = "unban", usage = "/<command> <player>", description = "Unbans a player, offline or online")
@CommandPermissions(permission = "plex.ban", source = RequiredCommandSource.ANY)

public class UnbanCMD extends PlexCommand
{
    @Override
    public Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args.length == 1)
        {
            PlexPlayer target = DataUtils.getPlayer(args[0]);

            if (target == null)
            {
                throw new PlayerNotFoundException();
            }

            plugin.getPunishmentManager().isAsyncBanned(target.getUuid()).whenComplete((aBoolean, throwable) ->
            {
                if (!aBoolean)
                {
                    send(sender, PlexUtils.mmDeserialize(new PlayerNotBannedException().getMessage()));
                    return;
                }
                plugin.getPunishmentManager().unban(target.getUuid());
                PlexUtils.broadcast(messageComponent("unbanningPlayer", sender.getName(), target.getName()));
            });
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
