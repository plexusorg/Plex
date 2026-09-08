package dev.plex.hook;

import com.oasis.api.Actor;
import com.oasis.api.Filter;
import com.oasis.api.Oasis;
import com.oasis.api.OasisApi;
import com.oasis.api.RollbackRequest;
import dev.plex.Plex;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OasisHook
{
    private final OasisApi api;
    private final Executor executor;

    public OasisHook(Plex plugin)
    {
        api = Oasis.api();
        executor = plugin.getIoExecutor();
    }

    public CompletableFuture<Integer> rollback(CommandSender sender, String playerName, int seconds)
    {
        UUID staff = sender instanceof Player player ? player.getUniqueId() : Actor.CONSOLE.id();
        long now = System.currentTimeMillis();
        Filter filter = Filter.ALL.withTime(now - seconds * 1000L, now);
        return CompletableFuture.supplyAsync(() -> api.resolvePlayer(playerName), executor).thenCompose(player ->
        {
            if (player.isEmpty())
            {
                return CompletableFuture.completedFuture(0);
            }
            RollbackRequest request = new RollbackRequest(player.get(), staff, filter, null);
            return api.rollbacks().rollback(request).completion().thenApply(progress ->
            {
                if (progress.aborted())
                {
                    throw new IllegalStateException("Oasis rollback aborted after " + progress.appliedRows() + " changes");
                }
                return Math.toIntExact(progress.appliedRows());
            });
        });
    }
}
