package dev.plex.hook;

import com.oasis.api.Actor;
import com.oasis.api.Oasis;
import com.oasis.api.OasisApi;
import com.oasis.api.Outcome;
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
        api = Oasis.api(plugin);
        this.plugin = plugin;
    }

    public CompletableFuture<Integer> rollback(CommandSender sender, String playerName, int seconds)
    {
        UUID staff = sender instanceof Player player ? player.getUniqueId() : Actor.CONSOLE.id();
        Instant now = Instant.ofEpochMilli(System.currentTimeMillis());
        Selection selection = Selection.all().since(now.minusSeconds(seconds)).until(now);
        return plugin.getPlayerService().findPlayer(playerName).thenCompose(player ->
        {
            if (player == null)
            {
                return CompletableFuture.completedFuture(0);
            }
            return api.operations().rollback(selection.by(player.getUuid())).requestedBy(staff).start().result().thenApply(result ->
            {
                if (result.outcome() != Outcome.FINISHED)
                {
                    throw new IllegalStateException("Oasis rollback " + result.outcome() + " after " + result.applied()
                            + " changes" + result.failure().map(failure -> ": " + failure.message()).orElse(""));
                }
                return Math.toIntExact(result.applied());
            });
        });
    }
}
