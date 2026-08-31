package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.punishment.Punishment;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
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
        command.executes(context -> executeCommand(context));
        command.then(word("username")
                .suggests(suggestPlayers())
                .executes(context -> executeCommand(context, string(context, "username")))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context,
                                argsWithGreedy(string(context, "username"), string(context, "reason"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        String username = args[0];
        if (!username.matches("[A-Za-z0-9_]{1,16}"))
        {
            return context.messageComponent("invalidUsername");
        }
        String reason = args.length > 1
                ? StringUtils.join(args, " ", 1, args.length)
                : context.messageString("noReasonProvided");
        if (!plugin.getPunishmentManager().banUsername(username, reason))
        {
            return context.messageComponent("nameAlreadyBanned");
        }

        PlexUtils.broadcast(context.messageComponent("banningName", context.senderName(), username));
        Component kickMessage = Punishment.generateIndefBanMessageWithReason(
                "username", plugin.config.getString("banning.ban_url"), reason);
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(username))
                .forEach(player -> plugin.getApi().scheduler().runEntity(player,
                        () -> BungeeUtil.kickPlayer(plugin, player, kickMessage)));
        return null;
    }
}
