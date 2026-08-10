package dev.plex.world;

import dev.plex.Plex;

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

    public static World generateConfigFlatWorld(Plex plugin, String name)
    {
        if (!plugin.config.contains("worlds." + name))
        {
            return null;
        }
        CustomWorld customWorld = new CustomWorld(name, new ConfigurationChunkGenerator(plugin, name))
        {
            @Override
            public World generate()
            {
                World world = super.generate();
                plugin.getWorldSpawnSignManager().ensureSign(world, name);
                return world;
            }
        };
        return customWorld.generate();
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
