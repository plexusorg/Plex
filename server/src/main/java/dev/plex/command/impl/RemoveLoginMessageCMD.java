package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandPermissions(permission = "plex.removeloginmessage", source = RequiredCommandSource.ANY)
@CommandParameters(name = "removeloginmessage", usage = "/<command> [-o <player>]", description = "Remove your own (or someone else's) login message", aliases = "rlm,removeloginmsg")
public class RemoveLoginMessageCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0 && !isConsole(sender))
        {
            if (playerSender != null)
            {
                PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
                plexPlayer.setLoginMessage("");
                return messageComponent("removedOwnLoginMessage");
            }
        }
        else if (args[0].equalsIgnoreCase("-o"))
        {
            checkPermission(sender, "plex.removeloginmessage.others");

            if (args.length < 2)
            {
                return messageComponent("specifyPlayer");
            }

            PlexPlayer plexPlayer = DataUtils.getPlayer(args[1]);
            if (plexPlayer == null)
            {
                return messageComponent("playerNotFound");
            }
            plexPlayer.setLoginMessage("");
            return messageComponent("removedOtherLoginMessage", plexPlayer.getName());
        }
        else
        {
            return messageComponent("noPermissionConsole");
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            if (silentCheckPermission(sender, "plex.removeloginmessage.others"))
            {
                return List.of("-o");
            }
        }
        return args.length == 2 && silentCheckPermission(sender, "plex.removeloginmessage.others") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}