package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandPermissions(permission = "plex.unfreeze")
@CommandParameters(name = "unfreeze", description = "Unfreeze a player", usage = "/<command> <player>")
public class UnfreezeCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            return usage();
        }
        PlexPlayer punishedPlayer = DataUtils.getPlayer(args[0]);
        if (punishedPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        if (!punishedPlayer.isFrozen())
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotFrozen"));
        }
        punishedPlayer.setFrozen(false);
        punishedPlayer.getPunishments().stream().filter(punishment -> punishment.getType() == PunishmentType.FREEZE && punishment.isActive()).forEach(punishment -> {
            punishment.setActive(false);
            plugin.getSqlPunishment().updatePunishment(punishment.getType(), false, punishment.getPunished());
        });
        PlexUtils.broadcast(messageComponent("unfrozePlayer", sender.getName(), punishedPlayer.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
