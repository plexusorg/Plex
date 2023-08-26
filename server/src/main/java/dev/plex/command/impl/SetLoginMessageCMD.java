package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.setloginmessage", source = RequiredCommandSource.ANY)
@CommandParameters(name = "setloginmessage", usage = "/<command> [-o <player>] <message>", description = "Sets your (or someone else's) login message", aliases = "slm,setloginmsg")
public class SetLoginMessageCMD extends PlexCommand
{
    private final boolean nameRequired = plugin.getConfig().getBoolean("loginmessages.name");
    private final boolean rankRequired = plugin.getConfig().getBoolean("loginmessages.rank");

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }
        if (playerSender != null)
        {
            if (args[0].equals("-o"))
            {
                checkPermission(sender, "plex.setloginmessage.others");

                if (args.length < 2)
                {
                    return messageComponent("specifyPlayer");
                }
                if (args.length < 3)
                {
                    return messageComponent("specifyLoginMessage");
                }
                PlexPlayer plexPlayer = DataUtils.getPlayer(args[1]);
                if (plexPlayer == null)
                {
                    return messageComponent("playerNotFound");
                }
                String message = StringUtils.join(args, " ", 2, args.length);
                message = message.replace(plexPlayer.getName(), "%player%");
                validateMessage(message);
                plexPlayer.setLoginMessage(message);
                return messageComponent("setOtherPlayersLoginMessage", plexPlayer.getName(),
                        message.replace("%player%", plexPlayer.getName()));
            }
            if (isConsole(sender))
            {
                return messageComponent("noPermissionConsole");
            }
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            String message = StringUtils.join(args, " ", 0, args.length);
            message = message.replace(plexPlayer.getName(), "%player%");
            message = PlexUtils.legacyToMiniString(message);
            validateMessage(message);
            plexPlayer.setLoginMessage(message);
            return messageComponent("setOwnLoginMessage",
                    message.replace("%player%", plexPlayer.getName()));
        }
        return null;
    }

    private void validateMessage(String message)
    {
        if (nameRequired && !message.contains("%player%"))
        {
            PlexLog.debug("Validating login message has a valid name in it");
            throw new CommandFailException(messageString("nameRequired"));
        }
    }
}