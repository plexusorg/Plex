package dev.plex.world;

import dev.plex.Plex;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.configuration.ConfigurationSection;

public final class WorldSpawnSignManager
{
    private static final int SIGN_X = 0;
    private static final int SIGN_Z = 0;
    private static final long WATCHDOG_PERIOD_TICKS = 20L;

    private final Plex plugin;
    private final Map<UUID, ProtectedSign> protectedSigns = new ConcurrentHashMap<>();
    private ScheduledTask watchdog;

    public WorldSpawnSignManager(Plex plugin)
    {
        this.plugin = plugin;
    }

    public void start()
    {
        if (watchdog != null)
        {
            return;
        }
        watchdog = plugin.getApi().scheduler().runGlobalTimer(task -> restoreLoadedWorlds(), 1L, WATCHDOG_PERIOD_TICKS);
    }

    public void stop()
    {
        if (watchdog != null)
        {
            watchdog.cancel();
            watchdog = null;
        }
        protectedSigns.clear();
    }

    public void ensureSign(World world, String configKey)
    {
        ProtectedSign protectedSign = protectedSigns.computeIfAbsent(world.getUID(), ignored -> locate(world, configKey));
        restoreNow(world, protectedSign);
    }

    public boolean isProtected(Block block)
    {
        ProtectedSign protectedSign = protectedSign(block.getWorld());
        if (protectedSign == null || block.getX() != SIGN_X || block.getZ() != SIGN_Z)
        {
            return false;
        }
        return block.getY() == protectedSign.y() || block.getY() == protectedSign.y() - 1;
    }

    public boolean isSign(Block block)
    {
        ProtectedSign protectedSign = protectedSign(block.getWorld());
        return protectedSign != null
                && block.getX() == SIGN_X
                && block.getY() == protectedSign.y()
                && block.getZ() == SIGN_Z;
    }

    public void restore(World world)
    {
        ProtectedSign protectedSign = protectedSign(world);
        if (protectedSign == null)
        {
            return;
        }
        plugin.getApi().scheduler().runRegionLater(
                world,
                SIGN_X >> 4,
                SIGN_Z >> 4,
                () -> restoreNow(world, protectedSign),
                1L);
    }

    private void restoreLoadedWorlds()
    {
        ConfigurationSection worlds = plugin.config.getConfigurationSection("worlds");
        if (worlds == null)
        {
            return;
        }
        for (String configKey : worlds.getKeys(false))
        {
            World world = Bukkit.getWorld(configKey);
            if (world == null)
            {
                continue;
            }
            plugin.getApi().scheduler().executeRegion(
                    world,
                    SIGN_X >> 4,
                    SIGN_Z >> 4,
                    () -> ensureSign(world, configKey));
        }
    }

    private ProtectedSign protectedSign(World world)
    {
        ProtectedSign existing = protectedSigns.get(world.getUID());
        if (existing != null)
        {
            return existing;
        }
        String configKey = configKey(world);
        if (configKey == null)
        {
            return null;
        }
        ProtectedSign located = locate(world, configKey);
        ProtectedSign raced = protectedSigns.putIfAbsent(world.getUID(), located);
        return raced == null ? located : raced;
    }

    private String configKey(World world)
    {
        ConfigurationSection worlds = plugin.config.getConfigurationSection("worlds");
        if (worlds == null)
        {
            return null;
        }
        return worlds.getKeys(false).stream()
                .filter(key -> key.equalsIgnoreCase(world.getName()))
                .findFirst()
                .orElse(null);
    }

    private ProtectedSign locate(World world, String configKey)
    {
        int signY = findSignY(world);
        if (signY == Integer.MIN_VALUE)
        {
            signY = world.getHighestBlockYAt(SIGN_X, SIGN_Z) + 1;
        }
        Block support = world.getBlockAt(SIGN_X, signY - 1, SIGN_Z);
        BlockData supportData = support.getType().isSolid()
                ? support.getBlockData().clone()
                : Material.BEDROCK.createBlockData();
        String displayName = plugin.config.getString("worlds." + configKey + ".name", configKey);
        return new ProtectedSign(signY, supportData, displayName);
    }

    private int findSignY(World world)
    {
        for (int y = world.getMaxHeight() - 1; y >= world.getMinHeight(); y--)
        {
            if (world.getBlockAt(SIGN_X, y, SIGN_Z).getState() instanceof Sign)
            {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    private void restoreNow(World world, ProtectedSign protectedSign)
    {
        Block support = world.getBlockAt(SIGN_X, protectedSign.y() - 1, SIGN_Z);
        if (!support.getType().isSolid())
        {
            support.setBlockData(protectedSign.supportData().clone(), false);
        }

        Block block = world.getBlockAt(SIGN_X, protectedSign.y(), SIGN_Z);
        boolean changed = false;
        if (!(block.getState() instanceof Sign))
        {
            block.setType(Material.OAK_SIGN, false);
            changed = true;
        }
        BlockState state = block.getState();
        if (!(state instanceof Sign sign))
        {
            return;
        }
        Component[] frontLines = {
                Component.empty(),
                Component.text(protectedSign.displayName()),
                Component.text("- 0, 0 -"),
                Component.empty()
        };
        for (int line = 0; line < frontLines.length; line++)
        {
            if (!sign.getSide(Side.FRONT).line(line).equals(frontLines[line]))
            {
                sign.getSide(Side.FRONT).line(line, frontLines[line]);
                changed = true;
            }
            if (!sign.getSide(Side.BACK).line(line).equals(Component.empty()))
            {
                sign.getSide(Side.BACK).line(line, Component.empty());
                changed = true;
            }
        }
        if (!sign.isWaxed())
        {
            sign.setWaxed(true);
            changed = true;
        }
        if (changed)
        {
            sign.update(true, false);
        }
    }

    private record ProtectedSign(int y, BlockData supportData, String displayName)
    {
    }
}
