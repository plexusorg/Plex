package dev.plex.command.impl;

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
        command.executes(context -> executeCommand(context));
        command.then(word("target")
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, string(context, "target")))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context,
                                argsWithGreedy(string(context, "target"), string(context, "reason"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        String ip = resolveIp(args[0]);
        if (ip == null)
        {
            return context.messageComponent("invalidIpOrPlayer");
        }
        String reason = args.length > 1
                ? StringUtils.join(args, " ", 1, args.length)
                : context.messageString("noReasonProvided");
        if (!plugin.getPunishmentManager().banIp(ip, reason))
        {
            return context.messageComponent("ipAlreadyBanned");
        }

        context.send(context.sender(), context.messageComponent("banningIp", context.senderName(), ip));
        Component kickMessage = Punishment.generateIndefBanMessageWithReason(
                "IP", plugin.config.getString("banning.ban_url"), reason);
        BanKickUtil.kickPlayersWithIp(plugin, ip, kickMessage);
        return null;
    }

    private String resolveIp(String target)
    {
        String candidate = target;
        if (candidate.length() > 1 && candidate.startsWith("[") && candidate.endsWith("]"))
        {
            candidate = candidate.substring(1, candidate.length() - 1);
        }
        if (InetAddresses.isInetAddress(candidate))
        {
            return BanDecisionService.canonicalIp(candidate);
        }

        PlexPlayer player = plugin.getPlayerService().getPlayer(target);
        if (player == null)
        {
            return null;
        }
        String ip = BanKickUtil.currentOrLastIp(player);
        return ip.isEmpty() ? null : ip;
    }
}
