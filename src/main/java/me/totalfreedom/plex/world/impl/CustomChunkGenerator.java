package me.totalfreedom.plex.world.impl;

import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Arrays;
import java.util.List;

public abstract class CustomChunkGenerator extends ChunkGenerator
{
    protected int height;
    private final List<BlockPopulator> populators;

    protected CustomChunkGenerator(int height, BlockPopulator... populators)
    {
        this.height = height;
        this.populators = Arrays.asList(populators);
    }

    public List<BlockPopulator> getDefaultPopulators(World world)
    {
        return populators;
    }

    public abstract void createLoopChunkData(int x, int y, int z, ChunkData chunk);
}