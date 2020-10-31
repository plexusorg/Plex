package me.totalfreedom.plex.world.impl;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.world.CustomWorld;
import me.totalfreedom.plex.world.FlatChunkGenerator;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
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
        World world = super.generate();
        Block block = world.getBlockAt(0, 51, 0);
        block.setType(Material.OAK_SIGN);
        BlockState state = block.getState();
        if (state instanceof Sign)
        {
            Sign sign = (Sign) state;

        }
        return world;
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