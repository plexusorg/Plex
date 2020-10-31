package me.totalfreedom.plex.world.impl;

import lombok.Getter;

public class OctaveOptions extends NoiseOptions
{
    @Getter
    private final int octaves;

    public OctaveOptions(int x, int y, double frequency, double amplitude, boolean normalized, int octaves)
    {
        super(x, y, frequency, amplitude, normalized);
        this.octaves = octaves;
    }
}