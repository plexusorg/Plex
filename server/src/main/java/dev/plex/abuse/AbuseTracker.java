package dev.plex.abuse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.time.Duration;
import java.util.UUID;

public final class AbuseTracker
{
    private final long windowNanos;
    private final long eventLimit;
    private final int strikesToEscalate;
    private final Cache<UUID, State> entries;

    public AbuseTracker(Duration window, long eventLimit, int strikesToEscalate, Duration idleExpiry, int maximumEntries)
    {
        windowNanos = window.toNanos();
        this.eventLimit = eventLimit;
        this.strikesToEscalate = strikesToEscalate;
        entries = CacheBuilder.newBuilder()
                .expireAfterAccess(idleExpiry)
                .maximumSize(maximumEntries)
                .build();
    }

    public Decision record(UUID uuid)
    {
        long now = System.nanoTime();
        State state = entries.asMap().computeIfAbsent(uuid, ignored -> new State(now));
        synchronized (state)
        {
            if (now - state.windowStartedAt >= windowNanos)
            {
                state.windowStartedAt = now;
                state.events = 0;
                state.thresholdReported = false;
            }
            boolean overLimit = ++state.events > eventLimit;
            boolean thresholdCrossed = overLimit && !state.thresholdReported;
            boolean escalationTriggered = false;
            if (thresholdCrossed)
            {
                state.thresholdReported = true;
                if (strikesToEscalate > 0 && ++state.strikes >= strikesToEscalate)
                {
                    state.strikes = 0;
                    escalationTriggered = true;
                }
            }
            return new Decision(!overLimit, thresholdCrossed, escalationTriggered);
        }
    }

    public void reset(UUID uuid)
    {
        entries.invalidate(uuid);
    }

    public record Decision(boolean allowed, boolean thresholdCrossed, boolean escalationTriggered)
    {
    }

    private static final class State
    {
        private long windowStartedAt;
        private long events;
        private int strikes;
        private boolean thresholdReported;

        private State(long now)
        {
            windowStartedAt = now;
        }
    }
}
