package dev.plex.storage;

import dev.plex.PlexBase;
import dev.plex.util.PlexLog;
import redis.clients.jedis.Jedis;

import java.util.function.Consumer;

public class RedisConnection implements PlexBase
{
    private Jedis jedis;

    public Jedis getJedis()
    {
        try
        {
            jedis = new Jedis(plugin.config.getString("data.side.hostname"),
                    plugin.config.getInt("data.side.port"));
            if (plugin.config.getBoolean("data.side.auth"))
            {
                jedis.auth(plugin.config.getString("data.side.password"));
            }
            return jedis;
        }
        catch (Exception ex)
        {
            PlexLog.error("An error occurred with Redis.");
            ex.printStackTrace();
        }
        return jedis;
    }

    public void runAsync(Consumer<Jedis> jedisConsumer)
    {
        new Thread(() ->
        {
            try (Jedis jedis = getJedis())
            {
                jedisConsumer.accept(jedis);
            }
        }).start();
    }

    public final boolean isEnabled()
    {
        return plugin.config.getBoolean("data.side.enabled");
    }
}
