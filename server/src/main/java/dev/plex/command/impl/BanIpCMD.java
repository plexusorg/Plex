package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.punishment.BanIpRange;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public class BanIpCMD extends ServerCommand
{
    public BanIpCMD()
    {
        super(command("banip")
                .description("Ban an IP address or range for 24 hours")
                .usage("/<command> <ip | range | player> [reason]")
                .permission("plex.banip")
                .build());
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(Commands.argument("target", new IpBanTargetArgument())
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "target"), null)))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext,
                                string(context, "target"), normalizeGreedyString(string(context, "reason")))))));
    }

    private Component executeTyped(ServerCommandContext context, String targetName, String suppliedReason)
    {
        IpBanTargetArgument.resolve(plugin, targetName).whenComplete((range, failure) ->
        {
            if (failure != null)
            {
                context.sender().sendMessage(playerLookupFailure(targetName, failure));
                return;
            }
            if (range == null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("invalidIpOrPlayer"));
                return;
            }
            banIp(context, range, suppliedReason);
        });
        return null;
    }

    private void banIp(ServerCommandContext context, BanIpRange range, String suppliedReason)
    {
        String ip = range.toString();
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        plugin.getPunishmentManager().banIp(range, reason, context.getUUID(context.sender()), context.senderName()).whenComplete((changed, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to ban IP " + ip, failure);
                context.sender().sendMessage(Component.text("Unable to complete the ban; check the server logs."));
            }
            else if (!changed)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("ipAlreadyBanned"));
            }
            else
            {
                context.sender().sendMessage(PlexUtils.messageComponent("banningIp", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("ip", ip)));
            }
        });
    }
}
