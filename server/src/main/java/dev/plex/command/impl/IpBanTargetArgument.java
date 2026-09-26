package dev.plex.command.impl;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.plex.Plex;
import dev.plex.punishment.BanIpRange;
import dev.plex.util.BanKickUtil;
import io.papermc.paper.command.brigadier.argument.CustomArgumentType;
import java.util.concurrent.CompletableFuture;

final class IpBanTargetArgument implements CustomArgumentType<String, String>
{
    @Override
    public String parse(StringReader reader)
    {
        int start = reader.getCursor();
        while (reader.canRead() && !Character.isWhitespace(reader.peek()))
        {
            reader.skip();
        }
        return reader.getString().substring(start, reader.getCursor());
    }

    @Override
    public StringArgumentType getNativeType()
    {
        // Accept IP punctuation in the client; split the target and reason on the server.
        return StringArgumentType.greedyString();
    }

    static CompletableFuture<BanIpRange> resolve(Plex plugin, String target)
    {
        if (target.contains(".") || target.contains(":") || target.contains("/") || target.contains("*"))
        {
            return CompletableFuture.completedFuture(parseRange(target));
        }
        return plugin.getPlayerService().resolveCommandPlayer(target).thenCompose(player ->
        {
            if (player == null)
            {
                return CompletableFuture.completedFuture(null);
            }
            return BanKickUtil.currentOrLastIp(plugin, player).thenApply(IpBanTargetArgument::parseRange);
        });
    }

    private static BanIpRange parseRange(String input)
    {
        try
        {
            return BanIpRange.parse(input);
        }
        catch (IllegalArgumentException exception)
        {
            return null;
        }
    }
}
