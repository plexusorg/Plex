package dev.plex.toml;

import java.util.concurrent.atomic.AtomicInteger;

interface ValueReader
{

    /**
     * @param s must already have been trimmed
     */
    boolean canRead(String s);

    /**
     * Partial validation. Stops after type terminator, rather than at EOI.
     *
     * @param s     must already have been validated by {@link #canRead(String)}
     * @param index where to start in s
     * @return a value or a {@link dev.plex.toml.Results.Errors}
     */
    Object read(String s, AtomicInteger index, dev.plex.toml.Context context);
}
