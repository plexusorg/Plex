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

public class UnbanNameCMD extends ServerCommand
{
    public UnbanNameCMD()
    {
        super(command("unbanname")
                .description("Remove a 24-hour username ban")
                .usage("/<command> <username>")
                .permission("plex.unbanname")
                .build());
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(word("username")
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "username")))));
    }

    private Component executeTyped(ServerCommandContext context, String suppliedUsername)
    {
        if (!suppliedUsername.matches("[A-Za-z0-9_]{1,16}"))
        {
            return PlexUtils.messageComponent("invalidUsername");
        }
        String username = suppliedUsername.toLowerCase(Locale.ROOT);
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(context.sender());
        plugin.getPunishmentManager().unbanUsername(username).whenComplete((changed, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to unban username " + username, failure);
                context.sender().sendMessage(Component.text("Unable to complete the unban; check the server logs."));
            }
            else if (!changed)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("nameNotBanned"));
            }
            else
            {
                broadcast.send(PlexUtils.messageComponent("unbanningName", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("username", username)));
            }
        });
        return null;
    }
}
