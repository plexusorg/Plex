package dev.plex.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import dev.plex.util.PlexUtils;
import dev.plex.punishment.IndefiniteIpRange;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.punishment.Punishment;
import dev.plex.util.BanKickUtil;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import net.kyori.adventure.text.Component;

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
        command.then(Commands.argument("target", new BanTargetArgument())
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "target"), null)))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext,
                                string(context, "target"), normalizeGreedyString(string(context, "reason")))))));
    }

    private static final class BanTargetArgument implements CustomArgumentType<String, String>
    {
        @Override
        public String parse(StringReader reader)
        {
            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) reader.skip();
            return reader.getString().substring(start, reader.getCursor());
        }

        @Override
        public StringArgumentType getNativeType()
        {
            // Accept IP punctuation in the client; split the target and reason on the server.
            return StringArgumentType.greedyString();
        }
    }

    private Component executeTyped(ServerCommandContext context, String targetName, String suppliedReason)
    {
        if (targetName.contains(".") || targetName.contains(":") || targetName.contains("/") || targetName.contains("*"))
        {
            IndefiniteIpRange range;
            try
            {
                range = IndefiniteIpRange.parse(targetName);
            }
            catch (IllegalArgumentException exception)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("invalidIpOrPlayer"));
                return null;
            }
            banIp(context, range, suppliedReason);
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
                else banIp(context, IndefiniteIpRange.parse(ip), suppliedReason);
            });
        });
        return null;
    }

    private void banIp(ServerCommandContext context, IndefiniteIpRange range, String suppliedReason)
    {
        String ip = range.toString();
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        if (!plugin.getPunishmentManager().banIp(ip, reason))
        {
            context.sender().sendMessage(PlexUtils.messageComponent("ipAlreadyBanned"));
            return;
        }

        context.sender().sendMessage(PlexUtils.messageComponent("banningIp", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("ip", ip)));
        Component kickMessage = Punishment.generateIndefBanMessageWithReason(
                "IP", plugin.config.getString("banning.ban_url"), reason);
        BanKickUtil.kickPlayersWithIp(plugin, ip, kickMessage);
    }
}
