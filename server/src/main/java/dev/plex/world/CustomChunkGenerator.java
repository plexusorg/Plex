package dev.plex.world;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Arrays;
import java.util.List;

public abstract class CustomChunkGenerator extends ChunkGenerator
{
    private final List<BlockPopulator> populators;
    protected int height;

    protected CustomChunkGenerator(int height, BlockPopulator... populators)
    {
        this.height = height;
        this.populators = Arrays.asList(populators);
    }

    @Override
    public List<BlockPopulator> getDefaultPopulators(World world)
    {
        return populators;
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}