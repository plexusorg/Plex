package me.totalfreedom.plex.world;

import me.totalfreedom.plex.Plex;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigurationChunkGenerator extends FlatChunkGenerator
{
    private static Plex plugin = Plex.get();

    private final String worldName;

    public ConfigurationChunkGenerator(String worldName, BlockPopulator... populators)
    {
        super(0, populators);
        this.worldName = worldName;
    }

    @Override
    public void createLoopChunkData(int x, int y, int z, ChunkData chunk)
    {
        int height = -1;
        Map<Material, Integer> blocks = new LinkedHashMap<>();
        for (String key : plugin.config.getConfigurationSection("worlds." + worldName + ".parameters").getKeys(false))
        {
            Material material = Material.getMaterial(key.toUpperCase());
            if (material == null) continue;
            int count = plugin.config.getInt("worlds." + worldName + ".parameters." + key);
            height += count;
            blocks.put(material, count);
        }
        for (Map.Entry<Material, Integer> entry : blocks.entrySet())
        {
            for (int i = 0; i < entry.getValue(); i++, height--)
                chunk.setBlock(x, height, z, entry.getKey());
        }
    }
}