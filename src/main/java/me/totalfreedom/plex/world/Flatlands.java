package me.totalfreedom.plex.world;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.world.impl.CustomWorld;
import me.totalfreedom.plex.world.impl.FlatChunkGenerator;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.generator.BlockPopulator;

import java.util.Random;

public class Flatlands extends CustomWorld
{
    private static Plex plugin = Plex.get();

    public Flatlands()
    {
        super("flatlands", new FlatlandsChunkGenerator());
    }

    @Override
    public World generate()
    {
        return super.generate();
    }

    private static class FlatlandsChunkGenerator extends FlatChunkGenerator
    {
        public FlatlandsChunkGenerator()
        {
            super(50, new FlatlandsBlockPopulator());
        }

        @Override
        public void createLoopChunkData(int x, int y, int z, ChunkData chunk)
        {
            int height = this.height;
            for (String key : plugin.config.getConfigurationSection("server.flatlands.parameters").getKeys(false))
            {
                Material material = Material.getMaterial(key.toUpperCase());
                if (material == null) continue;
                int count = plugin.config.getInt("server.flatlands.parameters." + key);
                for (int i = 0; i < count; i++, height--)
                    chunk.setBlock(x, height, z, material);
            }
        }

        private static class FlatlandsBlockPopulator extends BlockPopulator
        {
            @Override
            public void populate(World world, Random random, Chunk chunk)
            {
            }
        }
    }
}