package dev.plex.storage;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class StorageExecutor
{
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ExecutorService IO = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors() / 2), new ThreadFactory()
    {
        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable, "Plex Storage IO-" + THREAD_ID.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    });

    private StorageExecutor()
    {
    }

    public static Executor io()
    {
        return IO;
    }

    public static void shutdown()
    {
        IO.shutdownNow();
    }
}
