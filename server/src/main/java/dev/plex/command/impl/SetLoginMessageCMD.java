package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;


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
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(greedyString("message")
                .suggests((context, builder) ->
                {
                    if (!canUsePermission(context.getSource(), "plex.setloginmessage.others")) return builder.buildFuture();
                    String remaining = builder.getRemaining();
                    if (remaining.isBlank()) return builder.buildFuture();
                    String[] tokens = remaining.split("\\s+", -1);
                    if (tokens.length == 1 && tokens[0].startsWith("-")) return suggestMatching(builder, List.of("-o"));
                    if (tokens.length == 2 && tokens[0].equalsIgnoreCase("-o"))
                        return suggestLastGreedyToken(builder, PlexUtils.getPlayerNameList());
                    return builder.buildFuture();
                })
                .executes(context -> executeCommand(context, commandContext -> set(commandContext,
                        parseMessage(normalizeGreedyString(string(context, "message")))))));
    }

    private LoginMessageInput parseMessage(String message)
    {
        if (!message.equals("-o") && !message.startsWith("-o ")) return new LoginMessageInput(null, message);
        String[] fields = message.split(" ", 3);
        return new LoginMessageInput(fields.length > 1 ? fields[1] : null, fields.length > 2 ? fields[2] : null);
    }

    private Component set(ServerCommandContext context, LoginMessageInput input)
    {
        if (input.playerName() == null && input.message() != null) return setOwn(context, input.message());
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.setloginmessage.others");
        if (input.playerName() == null) return PlexUtils.messageComponent("specifyPlayer");
        if (input.message() == null) return PlexUtils.messageComponent("specifyLoginMessage");
        return setOther(context, input.playerName(), input.message());
    }

    private Component setOther(ServerCommandContext context, String playerName, String message)
    {
        plugin.getPlayerService().findPlayer(playerName).whenComplete((plexPlayer, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to load player {0}: {1}", playerName, failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to load the player."));
                return;
            }
            if (plexPlayer == null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("playerNotFound"));
                return;
            }
            String normalized = normalizeMessage(plexPlayer, message);
            try
            {
                validateMessage(context, plexPlayer, normalized);
            }
            catch (CommandFailException commandFailure)
            {
                context.sender().sendMessage(Component.text(commandFailure.getMessage()));
                return;
            }
            plexPlayer.setLoginMessage(normalized);
            plugin.getPlayerService().update(plexPlayer).whenComplete((unused, updateFailure) ->
            {
                if (updateFailure != null)
                {
                    PlexLog.warn("Unable to set login message for {0}: {1}", plexPlayer.getUuid(), updateFailure.getMessage());
                    context.sender().sendMessage(Component.text("Unable to save the login message."));
                }
                else context.sender().sendMessage(PlexUtils.messageComponent("setOtherPlayersLoginMessage", placeholder("player", plexPlayer.getName()), placeholder("message", MiniMessage.miniMessage().serialize(PlexUtils.stringToComponent(PlayerMeta.getLoginMessage(plugin.config, plexPlayer))))));
            });
        });
        return null;
    }

    private Component setOwn(ServerCommandContext context, String message)
    {
        Player playerSender = context.player();
        if (playerSender == null) return PlexUtils.messageComponent("noPermissionConsole");
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
        message = normalizeMessage(plexPlayer, message);
        validateMessage(context, plexPlayer, message);
        plexPlayer.setLoginMessage(message);
        plugin.getPlayerService().update(plexPlayer).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to set login message for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to save the login message."));
            }
            else context.sender().sendMessage(PlexUtils.messageComponent("setOwnLoginMessage", placeholder("message", PlexUtils.stringToComponent(PlayerMeta.getLoginMessage(plugin.config, plexPlayer)))));
        });
        return null;
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
            throw new CommandFailException(PlexUtils.messageString("nameRequired"));
        }
        if (!plugin.config.getBoolean("loginmessages.group"))
        {
            return;
        }
        if (PlayerMeta.getGroupTitle(plugin.config, plexPlayer).isEmpty())
        {
            throw new CommandFailException(PlexUtils.messageString("groupNotConfigured"));
        }
        if (!message.contains("%group%"))
        {
            PlexLog.debug("Validating login message has a valid group in it");
            throw new CommandFailException(PlexUtils.messageString("groupRequired"));
        }
    }

    private record LoginMessageInput(String playerName, String message) { }

}
