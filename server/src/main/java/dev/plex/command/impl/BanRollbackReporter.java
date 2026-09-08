package dev.plex.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

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
                sender.sendMessage(PlexUtils.messageComponent("rollbackError", Placeholder.parsed("error", String.valueOf(failure.getMessage()))));
            }
            else if (count == 0) sender.sendMessage(PlexUtils.messageComponent("rollbackNoResult"));
            else sender.sendMessage(PlexUtils.messageComponent("rollbackMessage", Placeholder.unparsed("count", String.valueOf(count))));
        });
    }
}
