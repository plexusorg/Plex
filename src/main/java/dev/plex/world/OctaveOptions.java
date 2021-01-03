package dev.plex.world;

import lombok.Getter;

@Getter
public class OctaveOptions extends NoiseOptions
{
    private final int octaves;

    public OctaveOptions(int x, int y, double frequency, double amplitude, boolean normalized, int octaves)
    {
        super(x, y, frequency, amplitude, normalized);
        this.octaves = octaves;
    }
}