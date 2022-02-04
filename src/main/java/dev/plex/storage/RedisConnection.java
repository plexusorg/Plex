package dev.plex.storage;

import dev.plex.PlexBase;
import dev.plex.util.PlexLog;
import redis.clients.jedis.Jedis;

public class RedisConnection extends PlexBase
{
    private Jedis jedis;

    /*public JedisPool openPool()
    {
        JedisPoolConfig jedisConfig = new JedisPoolConfig();
        //jedisConfig.setMaxIdle(10);
        //jedisConfig.setMaxTotal(100);
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RedisConnection.class.getClassLoader());
        this.pool = new JedisPool(jedisConfig, plugin.config.getString("data.side.hostname"),
                plugin.config.getInt("data.side.port"));
        Thread.currentThread().setContextClassLoader(previous);
        PlexLog.log("Connected to Redis!");
        return pool;
    }

    public Jedis getJedis()
    {
        try
        {
            this.jedis = pool.getResource();
        }
        catch (Exception ex)
        {
            PlexLog.error("An error occurred with Redis.");
            ex.printStackTrace();
        }
        if (plugin.config.getBoolean("data.side.auth"))
        {
            jedis.auth(plugin.config.getString("data.side.password"));
        }
        return jedis;
    }*/

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

    public final boolean isEnabled()
    {
        return plugin.config.getBoolean("data.side.enabled");
    }
}
