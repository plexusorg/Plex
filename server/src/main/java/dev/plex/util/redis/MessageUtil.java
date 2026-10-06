package dev.plex.util.redis;

import org.bukkit.Bukkit;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.punishment.BanIpRange;
import dev.plex.hook.VaultHook;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import static dev.plex.util.PlexUtils.messageComponent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

public final class MessageUtil
{
    private static final Gson GSON = new Gson();
    private static final String STAFF_CHAT_CHANNEL = "staffchat";
    private static final String INVALIDATION_CHANNEL = "plex:ban-cache-invalidation:v1";
    private static Plex plugin;
    private static String serverAddress;
    private static AutoCloseable subscription;
    private static volatile BanListeners banListeners;

    private MessageUtil()
    {
    }

    public static synchronized void subscribe(Plex currentPlugin)
    {
        close();
        if (!currentPlugin.getRedisConnection().isEnabled())
        {
            return;
        }
        plugin = currentPlugin;
        serverAddress = Bukkit.getServer().getIp() + ":" + Bukkit.getServer().getPort();
        subscription = currentPlugin.getRedisConnection().subscribe(MessageUtil::receive, MessageUtil::subscribed,
                STAFF_CHAT_CHANNEL, INVALIDATION_CHANNEL);
    }

    public static synchronized void close()
    {
        if (subscription != null)
        {
            try
            {
                subscription.close();
            }
            catch (Exception ex)
            {
                PlexLog.debug("Redis subscription close failed: {0}", ex.getMessage());
            }
        }
        subscription = null;
        banListeners = null;
        plugin = null;
        serverAddress = null;
    }

    // Redis calls resync after each subscription, because pub/sub drops invalidations sent while this server is disconnected.
    public static synchronized AutoCloseable onBanInvalidation(Consumer<BanCacheInvalidation> invalidation,
                                                               Runnable resync)
    {
        BanListeners listeners = new BanListeners(invalidation, resync);
        banListeners = listeners;
        return () ->
        {
            synchronized (MessageUtil.class)
            {
                if (banListeners == listeners)
                {
                    banListeners = null;
                }
            }
        };
    }

    public static CompletableFuture<Long> publishBanInvalidation(Plex plugin, UUID playerId, @Nullable String ip)
    {
        if (!plugin.getRedisConnection().isEnabled())
        {
            return CompletableFuture.completedFuture(0L);
        }
        JSONObject object = new JSONObject();
        object.put("playerId", playerId.toString());
        object.put("ip", ip == null ? JSONObject.NULL : BanIpRange.banMatchKey(ip));
        return publish(plugin, INVALIDATION_CHANNEL, object.toString(), "ban-cache invalidation");
    }

    public static void sendStaffChat(Plex plugin, CommandSender sender, Component message, UUID... ignore)
    {
        if (!plugin.getRedisConnection().isEnabled())
        {
            return;
        }
        JSONObject object = new JSONObject();
        object.put("sender", sender instanceof Player player ? player.getUniqueId().toString() : "");
        object.put("message", SafeMiniMessage.mmSerialize(message));
        object.put("ignore", GSON.toJson(ignore));
        object.put("server", serverAddress);
        publish(plugin, STAFF_CHAT_CHANNEL, object.toString(), "staff chat");
    }

    private static CompletableFuture<Long> publish(Plex plugin, String channel, String message, String description)
    {
        CompletableFuture<Long> result = plugin.getRedisConnection().publishAsync(channel, message);
        result.exceptionally(ex ->
        {
            PlexLog.warn("Could not publish {0}: {1}", description, ex.getMessage());
            return 0L;
        });
        return result;
    }

    private static void receive(String channel, String message)
    {
        Plex current = plugin;
        if (current != null)
        {
            dispatch(current, channel, message);
        }
    }

    private static void dispatch(Plex current, String channel, String message)
    {
        try
        {
            JSONObject object = new JSONObject(message);
            if (STAFF_CHAT_CHANNEL.equals(channel))
            {
                UUID[] ignore = GSON.fromJson(object.getString("ignore"), new TypeToken<UUID[]>() { }.getType());
                String sender = object.getString("sender").isEmpty() ? "CONSOLE" : object.getString("sender");
                Component prefix = sender.equals("CONSOLE")
                        ? PlexUtils.mmDeserialize("<dark_gray>[<dark_purple>Console<dark_gray>]")
                        : VaultHook.getPrefix(UUID.fromString(sender));
                Component chatMessage = SafeMiniMessage.mmDeserialize(object.getString("message"));
                boolean remote = !serverAddress.equalsIgnoreCase(object.getString("server"));
                Bukkit.getGlobalRegionScheduler().run(current, task ->
                {
                    PlexUtils.adminChat(sender, prefix, chatMessage, ignore);
                    if (remote)
                    {
                        current.getServer().getConsoleSender().sendMessage(
                                messageComponent("adminChatFormat", Placeholder.unparsed("sender", sender), Placeholder.component("prefix", prefix), Placeholder.component("message", chatMessage)));
                    }
                });
            }
            else if (INVALIDATION_CHANNEL.equals(channel) && banListeners instanceof BanListeners listeners)
            {
                listeners.invalidation().accept(new BanCacheInvalidation(
                        UUID.fromString(object.getString("playerId")),
                        object.isNull("ip") ? null : object.getString("ip")));
            }
        }
        catch (RuntimeException ex)
        {
            PlexLog.warn("Ignoring invalid Redis message on {0}: {1}", channel, ex.getMessage());
        }
    }

    private static void subscribed()
    {
        if (!(banListeners instanceof BanListeners listeners)) return;
        try
        {
            listeners.resync().run();
        }
        catch (RuntimeException ex)
        {
            PlexLog.error("Unable to resynchronize bans after the Redis subscription", ex);
        }
    }

    private record BanListeners(Consumer<BanCacheInvalidation> invalidation, Runnable resync)
    {
    }

    public record BanCacheInvalidation(UUID playerId, @Nullable String ip)
    {
    }
}
