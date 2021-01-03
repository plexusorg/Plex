package dev.plex.world;

import lombok.Getter;

@Getter
public class NoiseOptions
{
    private final int x;
    private final int y;
    private final double frequency;
    private final double amplitude;
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