package dev.plex.world;

import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

public abstract class FlatChunkGenerator extends CustomChunkGenerator
{
    public FlatChunkGenerator(int height, BlockPopulator... populators)
    {
        super(height, populators);
    }

    @Override
    public void generateSurface(WorldInfo worldInfo, Random random, int x, int z, ChunkData chunk)
    {
        for (int xx = 0; xx < 16; xx++)
        {
            for (int zz = 0; zz < 16; zz++)
            {
                createLoopChunkData(xx, height, zz, chunk);
            }
        }
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}