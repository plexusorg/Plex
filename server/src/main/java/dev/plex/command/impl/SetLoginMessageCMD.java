package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.meta.PlayerMeta;
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

public class SetLoginMessageCMD extends ServerCommand
{
    public SetLoginMessageCMD()
    {
        super(command("setloginmessage")
            .description("Sets your (or someone else's) login message")
            .usage("/<command> [-o <player>] <message>")
            .aliases("slm,setloginmsg")
            .permission("plex.setloginmessage")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(greedyString("message")
                .suggests((context, builder) ->
                {
                    if (!canUsePermission(context.getSource(), "plex.setloginmessage.others"))
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
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }
        if (args[0].equals("-o"))
        {
            context.checkPermission(sender, "plex.setloginmessage.others");

            if (args.length < 2)
            {
                return context.messageComponent("specifyPlayer");
            }
            if (args.length < 3)
            {
                return context.messageComponent("specifyLoginMessage");
            }
            PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[1]);
            if (plexPlayer == null)
            {
                return context.messageComponent("playerNotFound");
            }
            String message = StringUtils.join(args, " ", 2, args.length);
            message = normalizeMessage(plexPlayer, message);
            validateMessage(context, plexPlayer, message);
            plexPlayer.setLoginMessage(message);
            plugin.getPlayerService().update(plexPlayer);
            return context.messageComponent("setOtherPlayersLoginMessage", plexPlayer.getName(),
                    MiniMessage.miniMessage().serialize(PlexUtils.stringToComponent(PlayerMeta.getLoginMessage(plugin.config, plexPlayer))));
        }
        if (context.isConsole(sender))
        {
            return context.messageComponent("noPermissionConsole");
        }
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
        String message = normalizeMessage(plexPlayer, StringUtils.join(args, " ", 0, args.length));
        validateMessage(context, plexPlayer, message);
        plexPlayer.setLoginMessage(message);
        plugin.getPlayerService().update(plexPlayer);
        return context.messageComponent("setOwnLoginMessage", PlexUtils.stringToComponent(PlayerMeta.getLoginMessage(plugin.config, plexPlayer)));
    }

    private String normalizeMessage(PlexPlayer plexPlayer, String message)
    {
        String normalized = message.replace(plexPlayer.getName(), "%player%");
        String title = PlayerMeta.getGroupTitle(plugin.config, plexPlayer);
        return title.isEmpty() ? normalized : StringUtils.replaceIgnoreCase(normalized, title, "%group%");
    }

    private void validateMessage(ServerCommandContext context, PlexPlayer plexPlayer, String message)
    {
        if (plugin.config.getBoolean("loginmessages.name") && !message.contains("%player%"))
        {
            PlexLog.debug("Validating login message has a valid name in it");
            throw new CommandFailException(context.messageString("nameRequired"));
        }
        if (!plugin.config.getBoolean("loginmessages.group"))
        {
            return;
        }
        if (PlayerMeta.getGroupTitle(plugin.config, plexPlayer).isEmpty())
        {
            throw new CommandFailException(context.messageString("groupNotConfigured"));
        }
        if (!message.contains("%group%"))
        {
            PlexLog.debug("Validating login message has a valid group in it");
            throw new CommandFailException(context.messageString("groupRequired"));
        }
    }

}
