package dev.plex.util.function;

/**
 * @author Taah
 * @since 8:40 AM [02-09-2023]
 */
public interface QuadConsumer<K, V, S, T>
{
    void accept(K k, V v, S s, T t);
}
