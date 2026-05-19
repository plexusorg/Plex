package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

public class RollbackManager implements RollbackApi
{
    private static final List<String> ROLLBACK_ACTIONS = List.of("block-place", "block-break", "block-burn", "entity-spawn", "entity-kill", "entity-explode");

    private final Plex plugin;

    public RollbackManager(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable()
    {
        return (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
                || (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect());
    }

    @Override
    public boolean rollback(CommandSender sender, String playerName, int seconds)
    {
        if (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
        {
            plugin.getApi().scheduler().runGlobal(() -> rollbackWithPrism(sender, playerName, seconds));
            return true;
        }

        if (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect())
        {
            rollbackWithCoreProtect(playerName, seconds);
            return true;
        }

        return false;
    }

    private void rollbackWithPrism(CommandSender sender, String playerName, int seconds)
    {
        long now = Instant.now().getEpochSecond();
        ActivityQuery query = PaperActivityQuery.builder()
                .actionTypeKeys(ROLLBACK_ACTIONS)
                .causePlayerName(playerName)
                .before(now)
                .after(now - seconds)
                .rollback()
                .build();

        plugin.getPrismHook().getPrism().rollback(sender, query).whenComplete((result, error) ->
                plugin.getApi().scheduler().runGlobal(() ->
                {
                    if (error != null)
                    {
                        sendMessage(sender, PlexUtils.messageComponent("prismRollbackError", error.getMessage()));
                        PlexLog.error("Unable to rollback: {0}", error);
                        return;
                    }

                    int count = result.applied();
                    if (count == 0)
                    {
                        sendMessage(sender, PlexUtils.messageComponent("prismNoResult", count));
                        PlexLog.debug("No activities are available to rollback");
                        return;
                    }

                    sendMessage(sender, PlexUtils.messageComponent("prismRollbackMessage", count));
                    PlexLog.debug("Rolled back {0} activities", count);
                }));
    }

    private void rollbackWithCoreProtect(String playerName, int seconds)
    {
        plugin.getApi().scheduler().runAsync(() ->
                plugin.getCoreProtectHook().coreProtectAPI().performRollback(seconds, Collections.singletonList(playerName), null, null, null, null, 0, null));
    }

    private void sendMessage(CommandSender sender, Component message)
    {
        if (sender instanceof Player player)
        {
            plugin.getApi().scheduler().runEntity(player, () -> sender.sendMessage(message));
            return;
        }
        sender.sendMessage(message);
    }
}
