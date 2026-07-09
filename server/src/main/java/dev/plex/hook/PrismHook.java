package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.time.Instant;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

public class PrismHook
{
    private static final List<String> ROLLBACK_ACTIONS = List.of("block-place", "block-break", "block-burn", "entity-spawn", "entity-kill", "entity-explode");

    private final Plex plex;
    private RegisteredServiceProvider<PrismPaperApi> provider;

    public PrismHook(Plex plex)
    {
        this.plex = plex;
        Plugin plugin = plex.getServer().getPluginManager().getPlugin("Prism");

        // Check that Prism is loaded
        if (plugin != null && !plugin.isEnabled())
        {
            return;
        }

        provider = Bukkit.getServicesManager().getRegistration(PrismPaperApi.class);
    }

    public boolean hasPrism()
    {
        return provider != null;
    }

    public PrismPaperApi getPrism()
    {
        return provider.getProvider();
    }

    public void rollback(CommandSender sender, String playerName, int seconds)
    {
        long now = Instant.now().getEpochSecond();
        ActivityQuery query = PaperActivityQuery.builder()
                .actionTypeKeys(ROLLBACK_ACTIONS)
                .causePlayerName(playerName)
                .before(now)
                .after(now - seconds)
                .rollback()
                .build();

        getPrism().rollback(sender, query).whenComplete((result, error) ->
                plex.getApi().scheduler().runGlobal(() ->
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

    private void sendMessage(CommandSender sender, Component message)
    {
        if (sender instanceof Player player)
        {
            plex.getApi().scheduler().runEntity(player, () -> sender.sendMessage(message));
            return;
        }
        sender.sendMessage(message);
    }
}

