package dev.plex.world;

import dev.plex.Plex;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;

import java.util.LinkedHashMap;

public class ConfigurationChunkGenerator extends BlockMapChunkGenerator
{
    private static final Plex plugin = Plex.get();

    public ConfigurationChunkGenerator(String worldName, BlockPopulator... populators)
    {
        super(createBlockMap(worldName), populators);
    }

    private static LinkedHashMap<Material, Integer> createBlockMap(String worldName)
    {
        LinkedHashMap<Material, Integer> blockMap = new LinkedHashMap<>();
        for (String key : plugin.config.getConfigurationSection("worlds." + worldName + ".parameters").getKeys(false))
        {
            Material material = Material.getMaterial(key.toUpperCase());
            if (material == null)
            {
                continue;
            }
            int count = plugin.config.getInt("worlds." + worldName + ".parameters." + key);
            blockMap.put(material, count);
        }
        return blockMap;
    }
}