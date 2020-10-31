package me.totalfreedom.plex.world;

import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

public class CustomWorld extends WorldCreator
{
    private final CustomChunkGenerator chunks;

    public CustomWorld(String name, CustomChunkGenerator generator)
    {
        super(name);
        this.chunks = generator;
        this.generator(this.chunks);
    }

    @Override
    public ChunkGenerator generator()
    {
        return chunks;
    }

    public World generate()
    {
        return this.createWorld();
    }
}