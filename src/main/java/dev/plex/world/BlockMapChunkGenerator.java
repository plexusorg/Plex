package dev.plex.world;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;

import java.util.LinkedHashMap;
import java.util.Map;

public class BlockMapChunkGenerator extends FlatChunkGenerator
{
    protected LinkedHashMap<Material, Integer> blockMap;

    public BlockMapChunkGenerator(LinkedHashMap<Material, Integer> blockMap, BlockPopulator... populators)
    {
        super(0, populators);
        this.blockMap = blockMap;
    }

    @Override
    public void createLoopChunkData(int x, int y, int z, ChunkData chunk)
    {
        int height = -1;
        for (int i : blockMap.values())
        {
            height += i;
        }
        for (Map.Entry<Material, Integer> entry : blockMap.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++, height--)
            {
                chunk.setBlock(x, height, z, entry.getKey());
            }
        }
    }
}