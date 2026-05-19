package dev.plex.world;

import dev.plex.Plex;

import java.util.LinkedHashMap;

import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;

public class ConfigurationChunkGenerator extends BlockMapChunkGenerator
{
    public ConfigurationChunkGenerator(Plex plugin, String worldName, BlockPopulator... populators)
    {
        super(createBlockMap(plugin, worldName), populators);
    }

    private static LinkedHashMap<Material, Integer> createBlockMap(Plex plugin, String worldName)
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