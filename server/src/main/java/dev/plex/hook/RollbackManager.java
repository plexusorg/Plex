package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;

public class RollbackManager implements RollbackApi
{
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
    public CompletableFuture<Integer> rollback(CommandSender sender, String playerName, int seconds)
    {
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(playerName, "playerName");
        if (seconds <= 0) return CompletableFuture.failedFuture(new IllegalArgumentException("seconds must be positive"));
        if (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
        {
            CompletableFuture<Integer> result = new CompletableFuture<>();
            plugin.getApi().scheduler().runGlobal(() ->
            {
                try
                {
                    plugin.getPrismHook().rollback(sender, playerName, seconds).whenComplete((count, failure) ->
                    {
                        if (failure == null) result.complete(count); else result.completeExceptionally(failure);
                    });
                }
                catch (RuntimeException failure)
                {
                    result.completeExceptionally(failure);
                }
            });
            return result;
        }

        if (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect())
        {
            return CompletableFuture.supplyAsync(() -> plugin.getCoreProtectHook().coreProtectAPI()
                    .performRollback(seconds, Collections.singletonList(playerName), null, null, null, null, 0, null).size(),
                    plugin.getApi().scheduler().asyncExecutor());
        }

        return CompletableFuture.failedFuture(new IllegalStateException("No rollback integration is available"));
    }
}
