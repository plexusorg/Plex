package me.totalfreedom.plex.world;

import lombok.Getter;

public class NoiseOptions
{
    @Getter
    private final int x;
    @Getter
    private final int y;
    @Getter
    private final double frequency;
    @Getter
    private final double amplitude;
    @Getter
    private final boolean normalized;

    public NoiseOptions(int x, int y, double frequency, double amplitude, boolean normalized)
    {
        this.x = x;
        this.y = y;
        this.frequency = frequency;
        this.amplitude = amplitude;
        this.normalized = normalized;
    }
}