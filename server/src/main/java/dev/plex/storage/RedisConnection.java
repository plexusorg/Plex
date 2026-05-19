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
            jedis = new Jedis(plugin.config.getString("data.redis.hostname"),
                    plugin.config.getInt("data.redis.port"));
            if (plugin.config.getBoolean("data.redis.auth"))
            {
                jedis.auth(plugin.config.getString("data.redis.password"));
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
        return plugin.config.getBoolean("data.redis.enabled");
    }
}
