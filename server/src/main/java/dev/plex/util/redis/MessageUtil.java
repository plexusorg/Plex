package dev.plex.util.redis;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

import static dev.plex.util.PlexUtils.messageComponent;

public class MessageUtil
{
    private static final Gson GSON = new Gson();
    private static JedisPubSub SUBSCRIBER;

    public static void subscribe()
    {
        PlexLog.debug("Subscribing");
        SUBSCRIBER = new JedisPubSub()
        {
            @Override
            public void onMessage(String channel, String message)
            {
                try
                {
                    JSONObject object = new JSONObject(message);
                    if (channel.equalsIgnoreCase("staffchat"))
                    {
                        UUID[] ignore = GSON.fromJson(object.getString("ignore"), new TypeToken<UUID[]>()
                        {
                        }.getType());
                        String sender = object.getString("sender").isEmpty() ? "CONSOLE" : object.getString("sender");
                        PlexUtils.adminChat(sender, object.getString("message"), ignore);
                        String[] server = object.getString("server").split(":");
                        if (!Bukkit.getServer().getIp().equalsIgnoreCase(server[0]) || Bukkit.getServer().getPort() != Integer.parseInt(server[1]))
                        {
                            Plex.get().getServer().getConsoleSender().sendMessage(messageComponent("adminChatFormat", sender, object.getString("message")));
                        }
                    }
                }
                catch (JSONException ignored)
                {

                }
            }

            @Override
            public void onSubscribe(String channel, int subscribedChannels)
            {
                PlexLog.debug("Subscribed to {0}", channel);
            }
        };
        //        SUBSCRIBER.subscribe("staffchat", "chat");
        Plex.get().getRedisConnection().runAsync(jedis ->
        {
            jedis.subscribe(SUBSCRIBER, "staffchat", "chat");
        });
    }

    public static void sendStaffChat(CommandSender sender, Component message, UUID... ignore)
    {
        if (!Plex.get().getRedisConnection().isEnabled() || Plex.get().getRedisConnection().getJedis() == null)
        {
            return;
        }

        String miniMessage = SafeMiniMessage.mmSerialize(message);
        JSONObject object = new JSONObject();
        object.put("sender", sender instanceof Player player ? player.getName() : "");
        object.put("message", miniMessage);
        object.put("ignore", GSON.toJson(ignore));
        object.put("server", String.format("%s:%s", Bukkit.getServer().getIp(), Bukkit.getServer().getPort()));
        Plex.get().getRedisConnection().getJedis().publish("staffchat", object.toString());
    }
}
