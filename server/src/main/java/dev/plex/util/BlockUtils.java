package dev.plex.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class BlockUtils
{
    public static Location relative(Location origin, BlockFace face)
    {
        return origin.add(face.getModX(), face.getModY(), face.getModZ());
    }
}
