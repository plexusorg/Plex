package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import org.bukkit.command.CommandSender;

final class BanRollbackReporter
{
    private final Plex plugin;

    BanRollbackReporter(Plex plugin)
    {
        this.plugin = plugin;
    }

    void report(CommandSender sender, String playerName)
    {
        plugin.getApi().rollback().rollbackLastDay(sender, playerName).whenComplete((count, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to rollback {0}: {1}", playerName, failure.getMessage());
                sender.sendMessage(PlexUtils.messageComponent("prismRollbackError", placeholder("error", failure.getMessage())));
            }
            else if (count == 0) sender.sendMessage(PlexUtils.messageComponent("prismNoResult"));
            else sender.sendMessage(PlexUtils.messageComponent("prismRollbackMessage", placeholder("count", count)));
        });
    }
}
