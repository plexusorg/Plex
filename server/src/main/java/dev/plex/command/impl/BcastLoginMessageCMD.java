package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandPermissions(permission = "plex.broadcastloginmessage", source = RequiredCommandSource.ANY)
@CommandParameters(name = "bcastloginmessage", usage = "/<command> <player>", description = "Broadcast your login message (for vanish support)", aliases = "bcastlm")
public class BcastLoginMessageCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        PlexPlayer plexPlayer = DataUtils.getPlayer(args[0]);

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        String loginMessage = PlayerMeta.getLoginMessage(plexPlayer);
        if (!loginMessage.isEmpty())
        {
            PlexUtils.broadcast(PlexUtils.stringToComponent(loginMessage));
            PlexUtils.broadcast(mmString("<yellow>" + plexPlayer.getName() + " joined the game"));
        }
        else
        {
            return mmString("<red>This player does not have a login message.");
        }

        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}