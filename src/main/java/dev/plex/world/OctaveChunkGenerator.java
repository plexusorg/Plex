package dev.plex.world;

import java.util.Random;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.util.noise.PerlinOctaveGenerator;

public abstract class OctaveChunkGenerator extends CustomChunkGenerator
{
    private final OctaveOptions options;

    public OctaveChunkGenerator(int height, OctaveOptions options, BlockPopulator... populators)
    {
        super(height, populators);
        this.options = options;
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome)
    {
        ChunkData chunk = this.createChunkData(world);
        PerlinOctaveGenerator generator = new PerlinOctaveGenerator(new Random(world.getSeed()), options.getOctaves());
        for (int xx = 0; xx < 16; xx++)
        {
            for (int zz = 0; zz < 16; zz++)
            {
                height = (int)generator.noise(options.getX(), options.getY(), options.getFrequency(), options.getAmplitude(), options.isNormalized());
                createLoopChunkData(xx, height, zz, chunk);
            }
        }
        return chunk;
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}