package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import dev.plex.util.PlexUtils;
import com.google.common.net.InetAddresses;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.util.BanKickUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class BanIpCMD extends ServerCommand
{
    public BanIpCMD()
    {
        super(command("banip")
                .description("Indefinitely bans an IP address")
                .usage("/<command> <ip | player> [reason]")
                .permission("plex.banip")
                .build());
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(word("target")
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "target"), null)))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext,
                                string(context, "target"), normalizeGreedyString(string(context, "reason")))))));
    }

    private Component executeTyped(ServerCommandContext context, String targetName, String suppliedReason)
    {
        String candidate = targetName;
        if (candidate.length() > 1 && candidate.startsWith("[") && candidate.endsWith("]"))
        {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (InetAddresses.isInetAddress(candidate))
        {
            banIp(context, BanDecisionService.canonicalIp(candidate), suppliedReason);
            return null;
        }
        plugin.getPlayerService().findPlayer(targetName).whenComplete((player, failure) ->
        {
            if (failure != null)
            {
                context.sender().sendMessage(Component.text("Unable to load the player."));
                return;
            }
            if (player == null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("invalidIpOrPlayer"));
                return;
            }
            BanKickUtil.currentOrLastIp(plugin, player).thenAccept(ip ->
            {
                if (ip.isEmpty()) context.sender().sendMessage(PlexUtils.messageComponent("invalidIpOrPlayer"));
                else banIp(context, ip, suppliedReason);
            });
        });
        return null;
    }

    private void banIp(ServerCommandContext context, String ip, String suppliedReason)
    {
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        if (!plugin.getPunishmentManager().banIp(ip, reason))
        {
            context.sender().sendMessage(PlexUtils.messageComponent("ipAlreadyBanned"));
            return;
        }

        context.sender().sendMessage(PlexUtils.messageComponent("banningIp", placeholder("sender", context.senderName()), placeholder("ip", ip)));
        Component kickMessage = Punishment.generateIndefBanMessageWithReason(
                "IP", plugin.config.getString("banning.ban_url"), reason);
        BanKickUtil.kickPlayersWithIp(plugin, ip, kickMessage);
    }
}
