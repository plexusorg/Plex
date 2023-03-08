package dev.plex.world;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.PerlinOctaveGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

public abstract class OctaveChunkGenerator extends CustomChunkGenerator
{
    private final OctaveOptions options;

    public OctaveChunkGenerator(int height, OctaveOptions options, BlockPopulator... populators)
    {
        super(height, populators);
        this.options = options;
    }

    @Override
    public void generateNoise(@NotNull WorldInfo worldInfo, @NotNull Random random, int x, int z, @NotNull ChunkData chunk)
    {
        PerlinOctaveGenerator generator = new PerlinOctaveGenerator(new Random(worldInfo.getSeed()), options.getOctaves());
        for (int xx = 0; xx < 16; xx++)
        {
            for (int zz = 0; zz < 16; zz++)
            {
                height = (int) generator.noise(options.getX(), options.getY(), options.getFrequency(), options.getAmplitude(), options.isNormalized());
                createLoopChunkData(xx, height, zz, chunk);
            }
        }
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}