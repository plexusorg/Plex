package dev.plex.hook;

import dev.plex.Plex;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

public class PrismHook
{
    private static final List<String> ROLLBACK_ACTIONS = List.of("block-place", "block-break", "block-burn", "entity-spawn", "entity-kill", "entity-explode");

    private RegisteredServiceProvider<PrismPaperApi> provider;

    public PrismHook(Plex plex)
    {
        Plugin plugin = plex.getServer().getPluginManager().getPlugin("prism");

        if (plugin == null || !plugin.isEnabled())
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

    public CompletableFuture<Integer> rollback(CommandSender sender, String playerName, int seconds)
    {
        long now = Instant.now().getEpochSecond();
        ActivityQuery query = PaperActivityQuery.builder()
                .actionTypeKeys(ROLLBACK_ACTIONS)
                .causePlayerName(playerName)
                .before(now)
                .after(now - seconds)
                .rollback()
                .build();

        return getPrism().rollback(sender, query).thenApply(result -> result.applied()).toCompletableFuture();
    }
}
