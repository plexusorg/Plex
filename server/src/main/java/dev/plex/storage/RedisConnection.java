package dev.plex.storage;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public final class RedisConnection implements AutoCloseable
{
    private final Plex plugin;
    private final boolean enabled;
    private volatile boolean closed;
    private RedisSubscriber subscriber;

    public RedisConnection(Plex plugin)
    {
        this.plugin = plugin;
        enabled = plugin.config.getBoolean("data.redis.enabled");
    }

    public void execute(Consumer<Jedis> action)
    {
        try (Jedis jedis = open())
        {
            action.accept(jedis);
        }
    }

    public <T> T query(Function<Jedis, T> action)
    {
        try (Jedis jedis = open())
        {
            return action.apply(jedis);
        }
    }

    public CompletableFuture<Long> publishAsync(String channel, String message)
    {
        try
        {
            return CompletableFuture.supplyAsync(() -> query(jedis -> jedis.publish(channel, message)),
                    plugin.getApi().scheduler().asyncExecutor());
        }
        catch (RuntimeException ex)
        {
            return CompletableFuture.failedFuture(ex);
        }
    }

    public synchronized AutoCloseable subscribe(BiConsumer<String, String> listener, String... channels)
    {
        available();
        if (subscriber != null)
        {
            throw new IllegalStateException("Redis subscription already exists");
        }
        subscriber = new RedisSubscriber(listener, channels);
        subscriber.thread.start();
        return subscriber;
    }

    public void ping()
    {
        if (!"PONG".equals(query(Jedis::ping)))
        {
            throw new IllegalStateException("Unexpected Redis ping response");
        }
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public synchronized void close()
    {
        if (closed) return;
        closed = true;
        if (subscriber != null)
        {
            subscriber.close();
            subscriber = null;
        }
    }

    private Jedis open()
    {
        available();
        Jedis jedis = new Jedis(plugin.config.getString("data.redis.hostname"),
                plugin.config.getInt("data.redis.port"));
        if (plugin.config.getBoolean("data.redis.auth"))
        {
            jedis.auth(plugin.config.getString("data.redis.password"));
        }
        return jedis;
    }

    private void available()
    {
        if (!enabled) throw new IllegalStateException("Redis is disabled");
        if (closed) throw new IllegalStateException("Redis connection is closed");
    }

    private final class RedisSubscriber implements AutoCloseable, Runnable
    {
        private final BiConsumer<String, String> listener;
        private final String[] channels;
        private final Thread thread;
        private volatile boolean running = true;
        private volatile Jedis connection;

        private RedisSubscriber(BiConsumer<String, String> listener, String[] channels)
        {
            if (channels.length == 0) throw new IllegalArgumentException("At least one Redis channel is required");
            this.listener = listener;
            this.channels = channels.clone();
            thread = Thread.ofPlatform().daemon().name("Plex-Redis-Subscriber").unstarted(this);
        }

        @Override
        public void run()
        {
            while (running && !closed)
            {
                try (Jedis jedis = open())
                {
                    connection = jedis;
                    JedisPubSub subscription = new JedisPubSub()
                    {
                        @Override
                        public void onMessage(String channel, String message)
                        {
                            listener.accept(channel, message);
                        }
                    };
                    jedis.subscribe(subscription, channels);
                }
                catch (RuntimeException ex)
                {
                    if (running && !closed) PlexLog.debug("Redis subscription disconnected: {0}", ex.getMessage());
                }
                finally
                {
                    connection = null;
                }
                if (running && !closed)
                {
                    try
                    {
                        Thread.sleep(5_000L);
                    }
                    catch (InterruptedException ex)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }

        @Override
        public void close()
        {
            running = false;
            Jedis currentConnection = connection;
            if (currentConnection != null) currentConnection.close();
            thread.interrupt();
        }
    }
}
