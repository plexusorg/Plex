package dev.plex.world;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.noise.PerlinNoiseGenerator;

import java.util.Random;

public abstract class NoiseChunkGenerator extends CustomChunkGenerator
{
    private final NoiseOptions options;

    public NoiseChunkGenerator(int height, NoiseOptions options, BlockPopulator... populators)
    {
        super(height, populators);
        this.options = options;
    }

    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunk)
    {
        PerlinNoiseGenerator generator = new PerlinNoiseGenerator(new Random(worldInfo.getSeed()));
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