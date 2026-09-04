package dev.plex.util.redis;

import org.bukkit.Bukkit;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
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
import static dev.plex.api.message.MessagePlaceholder.placeholder;

public final class MessageUtil
{
    private static final Gson GSON = new Gson();
    private static final String STAFF_CHAT_CHANNEL = "staffchat";
    private static final String INVALIDATION_CHANNEL = "plex:ban-cache-invalidation:v1";
    private static Plex plugin;
    private static String serverAddress;
    private static AutoCloseable subscription;
    private static Consumer<BanCacheInvalidation> invalidationListener;

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
        subscription = currentPlugin.getRedisConnection().subscribe(MessageUtil::receive,
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
        invalidationListener = null;
        plugin = null;
        serverAddress = null;
    }

    public static synchronized AutoCloseable onBanInvalidation(Consumer<BanCacheInvalidation> listener)
    {
        invalidationListener = listener;
        return () ->
        {
            synchronized (MessageUtil.class)
            {
                if (invalidationListener == listener)
                {
                    invalidationListener = null;
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
        object.put("ip", ip == null ? JSONObject.NULL : ip);
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
                String prefix = sender.equals("CONSOLE")
                        ? "<dark_gray>[<dark_purple>Console<dark_gray>]"
                        : PlexUtils.mmSerialize(VaultHook.getPrefix(UUID.fromString(sender)));
                String chatMessage = object.getString("message");
                boolean remote = !serverAddress.equalsIgnoreCase(object.getString("server"));
                Bukkit.getGlobalRegionScheduler().run(current, task ->
                {
                    PlexUtils.adminChat(sender, prefix, chatMessage, ignore);
                    if (remote)
                    {
                        current.getServer().getConsoleSender().sendMessage(
                                messageComponent("adminChatFormat", placeholder("sender", sender), placeholder("prefix", prefix), placeholder("message", chatMessage)));
                    }
                });
            }
            else if (INVALIDATION_CHANNEL.equals(channel) && invalidationListener != null)
            {
                invalidationListener.accept(new BanCacheInvalidation(
                        UUID.fromString(object.getString("playerId")),
                        object.isNull("ip") ? null : object.getString("ip")));
            }
        }
        catch (RuntimeException ex)
        {
            PlexLog.warn("Ignoring invalid Redis message on {0}: {1}", channel, ex.getMessage());
        }
    }

    public record BanCacheInvalidation(UUID playerId, @Nullable String ip)
    {
    }
}
