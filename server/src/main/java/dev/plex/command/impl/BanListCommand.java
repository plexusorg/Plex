package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import dev.plex.util.PlexUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.punishment.Punishment;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import dev.plex.util.PlexLog;
import java.util.concurrent.CompletableFuture;

public class BanListCommand extends ServerCommand
{
    public BanListCommand()
    {
        super(command("banlist")
            .description("Manages the banlist")
            .usage("/<command> [purge]")
            .permission("plex.banlist")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, this::list));
        command.then(literal("purge")
                .executes(context -> executeCommand(context, this::clear)));
        command.then(literal("clear")
                .executes(context -> executeCommand(context, this::clear)));
    }

    private Component list(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
        {
            if (throwable != null)
            {
                PlexLog.warn("Unable to load active bans: {0}", throwable.getMessage());
                sender.sendMessage(Component.text("Unable to load active bans."));
                return;
            }
            String names = StringUtils.join(punishments.stream()
                    .map(punishment -> StringUtils.defaultIfBlank(punishment.getResolvedPunishedName(),
                            punishment.getPunished().toString())).toList(), ", ");
            sender.sendMessage(PlexUtils.messageComponent("activeBansList", placeholder("count", punishments.size()), placeholder("players", names)));
        });
        return null;
    }

    private Component clear(ServerCommandContext context)
    {
        if (!context.isConsole()) throw new ConsoleOnlyException();
        CommandSender sender = context.sender();
        plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
        {
            if (throwable != null)
            {
                sender.sendMessage(Component.text("Unable to load active bans."));
                return;
            }
            var uuids = punishments.stream().map(Punishment::getPunished).distinct().toList();
            CompletableFuture.allOf(uuids.stream().map(plugin.getPunishmentManager()::unban)
                            .toArray(CompletableFuture[]::new))
                    .whenComplete((unused, failure) ->
                    {
                        if (failure != null) sender.sendMessage(Component.text("Unable to clear all active bans."));
                        else sender.sendMessage(PlexUtils.messageComponent("unbannedPlayers", placeholder("count", uuids.size())));
                    });
        });
        return null;
    }

}
