package dev.plex.command.impl;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.punishment.Punishment;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class BanNameCMD extends ServerCommand
{
    public BanNameCMD()
    {
        super(command("banname")
                .description("Indefinitely bans a username")
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
        String username = usernameName;
        if (!username.matches("[A-Za-z0-9_]{1,16}"))
        {
            return PlexUtils.messageComponent("invalidUsername");
        }
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        if (!plugin.getPunishmentManager().banUsername(username, reason))
        {
            return PlexUtils.messageComponent("nameAlreadyBanned");
        }

        PlexUtils.broadcast(PlexUtils.messageComponent("banningName", context.senderName(), username));
        Component kickMessage = Punishment.generateIndefBanMessageWithReason(
                "username", plugin.config.getString("banning.ban_url"), reason);
        plugin.getPlayerService().cachedPlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(username))
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(java.util.Objects::nonNull)
                .forEach(player -> player.getScheduler().run(plugin,
                        task -> BungeeUtil.kickPlayer(plugin, player, kickMessage), null));
        return null;
    }
}
