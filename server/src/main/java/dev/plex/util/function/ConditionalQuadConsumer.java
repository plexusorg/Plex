package dev.plex.util.function;

public interface ConditionalQuadConsumer<K, V, S, T>
{
    boolean accept(K k, V v, S s, T t);
}
