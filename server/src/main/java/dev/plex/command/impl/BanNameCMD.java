package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.CapturedActionBroadcast;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class BanNameCMD extends ServerCommand
{
    public BanNameCMD()
    {
        super(command("banname")
                .description("Ban a username for 24 hours")
                .usage("/<command> <username> [reason]")
                .permission("plex.banname")
                .build());
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(word("username")
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "username"), null)))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext,
                                string(context, "username"), normalizeGreedyString(string(context, "reason")))))));
    }

    private Component executeTyped(ServerCommandContext context, String usernameName, String suppliedReason)
    {
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(context.sender());
        if (!usernameName.matches("[A-Za-z0-9_]{1,16}"))
        {
            return PlexUtils.messageComponent("invalidUsername");
        }
        String username = usernameName.toLowerCase(Locale.ROOT);
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        plugin.getPunishmentManager().banUsername(username, reason, context.getUUID(context.sender()), context.senderName()).whenComplete((changed, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to ban username " + username, failure);
                context.sender().sendMessage(Component.text("Unable to complete the ban; check the server logs."));
            }
            else if (!changed)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("nameAlreadyBanned"));
            }
            else
            {
                broadcast.send(PlexUtils.messageComponent("banningName", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("username", username)));
            }
        });
        return null;
    }
}
