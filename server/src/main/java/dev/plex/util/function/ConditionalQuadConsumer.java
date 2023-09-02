package dev.plex.util.function;

/**
 * @author Taah
 * @since 8:40 AM [02-09-2023]
 */
public interface ConditionalQuadConsumer<K, V, S, T>
{
    boolean accept(K k, V v, S s, T t);
}
