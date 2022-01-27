package dev.plex.world;

import java.util.Random;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;

public abstract class FlatChunkGenerator extends CustomChunkGenerator
{
    public FlatChunkGenerator(int height, BlockPopulator... populators)
    {
        super(height, populators);
    }

    @Override
    public ChunkData generateChunkData(World world, Random random, int x, int z, BiomeGrid biome)
    {
        ChunkData chunk = this.createChunkData(world);
        for (int xx = 0; xx < 16; xx++)
        {
            for (int zz = 0; zz < 16; zz++)
            {
                createLoopChunkData(xx, height, zz, chunk);
            }
        }
        return chunk;
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}