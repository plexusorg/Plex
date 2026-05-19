package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;

import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.setloginmessage", source = RequiredCommandSource.ANY)
@CommandParameters(name = "setloginmessage", usage = "/<command> [-o <player>] <message>", description = "Sets your (or someone else's) login message", aliases = "slm,setloginmsg")
public class SetLoginMessageCMD extends ServerCommand
{
    private final boolean nameRequired = plugin.getConfig().getBoolean("loginmessages.name");

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(greedyString("message")
                .suggests((context, builder) ->
                {
                    if (!silentCheckPermission(context.getSource().getSender(), "plex.setloginmessage.others"))
                    {
                        return builder.buildFuture();
                    }

                    String remaining = builder.getRemaining();
                    if (remaining.isBlank())
                    {
                        return builder.buildFuture();
                    }

                    String[] tokens = remaining.split("\\s+", -1);
                    if (tokens.length == 1 && tokens[0].startsWith("-"))
                    {
                        return suggestMatching(builder, List.of("-o"));
                    }
                    if (tokens.length == 2 && tokens[0].equalsIgnoreCase("-o"))
                    {
                        return suggestLastGreedyToken(builder, PlexUtils.getPlayerNameList());
                    }
                    return builder.buildFuture();
                })
                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "message")))));
    }

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
                PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[1]);
                if (plexPlayer == null)
                {
                    return messageComponent("playerNotFound");
                }
                String message = StringUtils.join(args, " ", 2, args.length);
                message = message.replace(plexPlayer.getName(), "%player%");
                validateMessage(message);
                plexPlayer.setLoginMessage(message);
                return messageComponent("setOtherPlayersLoginMessage", plexPlayer.getName(),
                        MiniMessage.miniMessage().serialize(PlexUtils.stringToComponent(message.replace("%player%", plexPlayer.getName()))));
            }
            if (isConsole(sender))
            {
                return messageComponent("noPermissionConsole");
            }
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            String message = StringUtils.join(args, " ", 0, args.length)
                    .replace(plexPlayer.getName(), "%player%");
            validateMessage(message);
            plexPlayer.setLoginMessage(message);
            return messageComponent("setOwnLoginMessage", PlexUtils.stringToComponent(message.replace("%player%", plexPlayer.getName())));
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
