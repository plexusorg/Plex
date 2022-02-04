package dev.plex.storage;

import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.util.PlexLog;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisConnection extends PlexBase
{
    private JedisPool pool;
    private Jedis jedis;

    public JedisPool openPool()
    {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RedisConnection.class.getClassLoader());
        this.pool = new JedisPool(plugin.config.getString("data.side.hostname"), Plex.get().getConfig().getInt("data.side.port"));
        Thread.currentThread().setContextClassLoader(previous);
        PlexLog.log("Connected to Redis!");
        return pool;
    }

    public Jedis getJedis()
    {
        this.jedis = pool.getResource();
        if (plugin.config.getBoolean("data.side.auth"))
        {
            jedis.auth(plugin.config.getString("data.side.password"));
        }
        return jedis;
    }

    public final boolean isEnabled()
    {
        return plugin.config.getBoolean("data.side.enabled");
    }
}
