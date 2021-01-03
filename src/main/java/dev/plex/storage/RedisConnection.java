package dev.plex.storage;

import dev.plex.Plex;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisConnection
{
    private JedisPool pool;
    private Jedis jedis;

    public JedisPool openPool()
    {
        ClassLoader previous = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RedisConnection.class.getClassLoader());
        this.pool = new JedisPool(new JedisPoolConfig(), Plex.get().getConfig().getString("data.side.hostname"), Plex.get().getConfig().getInt("data.side.port"));
        Thread.currentThread().setContextClassLoader(previous);
        return pool;
    }

    public Jedis getJedis()
    {
        this.jedis = pool.getResource();
        if (Plex.get().getConfig().getBoolean("data.side.auth"))
        {
            jedis.auth(Plex.get().getConfig().getString("data.side.password"));
        }
        return jedis;
    }

}
