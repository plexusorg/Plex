package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import dev.plex.util.PlexLog;

public class BanListCommand extends ServerCommand
{
    public BanListCommand()
    {
        super(command("banlist")
            .description("Manages the banlist")
            .usage("/<command> [purge]")
            .permission("plex.banlist")
            .source(RequiredCommandSource.CONSOLE)
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("purge")
                .executes(context -> executeCommand(context, "purge")));
        command.then(literal("clear")
                .executes(context -> executeCommand(context, "clear")));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        String[] args = context.args();
        if (args.length == 0)
        {
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                if (throwable != null)
                {
                    PlexLog.warn("Unable to load active bans: {0}", throwable.getMessage());
                    context.send(sender, Component.text("Unable to load active bans."));
                    return;
                }
                String names = StringUtils.join(punishments.stream()
                        .map(punishment -> StringUtils.defaultIfBlank(punishment.getResolvedPunishedName(),
                                punishment.getPunished().toString())).toList(), ", ");
                context.send(sender, context.messageComponent("activeBansList", punishments.size(), names));
            });
            return null;
        }
        plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
        {
            if (throwable != null)
            {
                context.send(sender, Component.text("Unable to load active bans."));
                return;
            }
            var uuids = punishments.stream().map(dev.plex.punishment.Punishment::getPunished).distinct().toList();
            java.util.concurrent.CompletableFuture.allOf(uuids.stream().map(plugin.getPunishmentManager()::unban)
                            .toArray(java.util.concurrent.CompletableFuture[]::new))
                    .whenComplete((unused, failure) ->
                    {
                        if (failure != null) context.send(sender, Component.text("Unable to clear all active bans."));
                        else context.send(sender, context.messageComponent("unbannedPlayers", uuids.size()));
                    });
        });
        return null;
    }

}
