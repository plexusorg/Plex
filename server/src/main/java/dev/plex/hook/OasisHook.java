package dev.plex.hook;

import com.oasis.api.Actor;
import com.oasis.api.Oasis;
import com.oasis.api.OasisApi;
import com.oasis.api.Outcome;
import com.oasis.api.RollbackRequest;
import com.oasis.api.Selection;
import dev.plex.Plex;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class OasisHook
{
    private final OasisApi api;
    private final Plex plugin;

    public OasisHook(Plex plugin)
    {
        api = Oasis.api();
        this.plugin = plugin;
    }

    public CompletableFuture<Integer> rollback(CommandSender sender, String playerName, int seconds)
    {
        UUID staff = sender instanceof Player player ? player.getUniqueId() : Actor.CONSOLE.id();
        Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
        Selection selection = Selection.edits().between(now.minusSeconds(seconds), now);
        return api.players().resolve(playerName).thenCompose(player ->
        {
            if (player.isEmpty())
            {
                return CompletableFuture.completedFuture(0);
            }
            RollbackRequest request = RollbackRequest.rollback(plugin, selection.players(player.get())).requestedBy(staff);
            return api.rollbacks().start(request).result().thenApply(result ->
            {
                if (result.outcome() != Outcome.FINISHED)
                {
                    throw new IllegalStateException("Oasis rollback " + result.outcome() + " after " + result.applied()
                            + " changes" + result.failureMessage().map(message -> ": " + message).orElse(""));
                }
                return Math.toIntExact(result.applied());
            });
        });
    }
}
