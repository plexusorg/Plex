package dev.plex.storage;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import redis.clients.jedis.Jedis;

import java.util.function.Consumer;
import java.util.function.Function;

public class RedisConnection
{
    private final Plex plugin;
    private Jedis jedis;

    public RedisConnection(Plex plugin)
    {
        this.plugin = plugin;
    }

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

    public void execute(Consumer<Jedis> jedisConsumer)
    {
        try (Jedis jedis = getJedis())
        {
            jedisConsumer.accept(jedis);
        }
    }

    public <T> T query(Function<Jedis, T> jedisFunction)
    {
        try (Jedis jedis = getJedis())
        {
            return jedisFunction.apply(jedis);
        }
    }

    public void runAsync(Consumer<Jedis> jedisConsumer)
    {
        StorageExecutor.io().execute(() -> execute(jedisConsumer));
    }

    public final boolean isEnabled()
    {
        return plugin.config.getBoolean("data.redis.enabled");
    }
}
