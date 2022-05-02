package dev.plex.util;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public class BlockUtils
{
    public static Location relative(Location origin, BlockFace face)
    {
        return switch (face)
                {
                    case UP -> origin.add(0, 1, 0);
                    case DOWN -> origin.subtract(0, 1, 0);
                    case NORTH -> origin.subtract(0, 0, 1);
                    case SOUTH -> origin.add(0, 0, 1);
                    case WEST -> origin.subtract(1, 0, 0);
                    case EAST -> origin.add(1, 0, 0);
                    default -> origin.add(face.getModX(), face.getModY(), face.getModZ());
                };
    }
}
