package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.CapturedActionBroadcast;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class UnbanIpCMD extends ServerCommand
{
    public UnbanIpCMD()
    {
        super(command("unbanip")
                .description("Remove a 24-hour IP address or range ban")
                .usage("/<command> <ip | range | player>")
                .permission("plex.unbanip")
                .build());
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(Commands.argument("target", new IpBanTargetArgument())
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "target")))));
    }

    private Component executeTyped(ServerCommandContext context, String targetName)
    {
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(context.sender());
        IpBanTargetArgument.resolve(plugin, targetName).whenComplete((range, lookupFailure) ->
        {
            if (lookupFailure != null)
            {
                context.sender().sendMessage(playerLookupFailure(targetName, lookupFailure));
                return;
            }
            if (range == null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("invalidIpOrPlayer"));
                return;
            }
            plugin.getPunishmentManager().unbanIp(range).whenComplete((changed, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.error("Unable to unban IP " + range, failure);
                    context.sender().sendMessage(Component.text("Unable to complete the unban; check the server logs."));
                }
                else if (!changed)
                {
                    context.sender().sendMessage(PlexUtils.messageComponent("ipNotBanned"));
                }
                else
                {
                    broadcast.send(PlexUtils.messageComponent("unbanningIp", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("ip", range.toString())));
                }
            });
        });
        return null;
    }
}
