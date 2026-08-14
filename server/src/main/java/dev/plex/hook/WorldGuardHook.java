package dev.plex.hook;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.RegionGroup;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import dev.plex.Plex;
import dev.plex.config.Config;
import dev.plex.util.PlexLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/**
 * Optional WorldGuard integration used by the {@code /protect} command.
 *
 * <p>This class must only be loaded after WorldGuard has been detected. Keeping
 * all WorldGuard and WorldEdit references here allows Plex to start normally
 * when neither plugin is installed.</p>
 */
public final class WorldGuardHook
{
    private final Config config;

    public WorldGuardHook(Plex plugin)
    {
        config = new Config(plugin, "protection.yml");
        config.load(false);
        synchronizeConfiguredRegions();
    }

    public Config config()
    {
        return config;
    }

    public Collection<String> presetNames()
    {
        ConfigurationSection presets = config.getConfigurationSection("presets");
        return presets == null ? List.of() : presets.getKeys(false);
    }

    public Collection<String> managedRegionNames()
    {
        ConfigurationSection regions = config.getConfigurationSection("regions");
        return regions == null ? List.of() : regions.getKeys(false);
    }

    public Collection<String> regionNames(World world)
    {
        RegionManager manager = getRegionManager(world);
        return manager == null ? List.of() : manager.getRegions().keySet();
    }

    public ProtectedRegion createFromSelection(Player player, String id, String preset) throws IncompleteRegionException
    {
        Region selection = WorldEdit.getInstance().getSessionManager()
                .get(BukkitAdapter.adapt(player))
                .getSelection(BukkitAdapter.adapt(player.getWorld()));
        return create(player.getWorld(), id, preset, selection.getMinimumPoint(), selection.getMaximumPoint());
    }

    public ProtectedRegion createAround(Player player, String id, String preset, int radius)
    {
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        World world = player.getWorld();
        BlockVector3 minimum = BlockVector3.at(x - radius, world.getMinHeight(), z - radius);
        BlockVector3 maximum = BlockVector3.at(x + radius, world.getMaxHeight() - 1, z + radius);
        return create(world, id, preset, minimum, maximum);
    }

    public ProtectedRegion create(World world, String id, String preset, BlockVector3 minimum, BlockVector3 maximum)
    {
        validateRegionId(id);
        RegionManager manager = requireRegionManager(world);
        if (manager.hasRegion(id))
        {
            throw new ProtectionException("region-exists", id);
        }

        List<ConfiguredFlag> flags = readPreset(preset);
        ProtectedRegion region = new ProtectedCuboidRegion(id, minimum, maximum);
        applyFlags(region, flags);
        manager.addRegion(region);
        saveManagedRegion(world, region, preset, flags);
        return region;
    }

    public ProtectedRegion applyPreset(World world, String id, String preset)
    {
        RegionManager manager = requireRegionManager(world);
        ProtectedRegion region = manager.getRegion(id);
        if (region == null)
        {
            throw new ProtectionException("region-not-found", id);
        }
        if (!(region instanceof ProtectedCuboidRegion))
        {
            throw new ProtectionException("unsupported-region", id);
        }
        String managedWorld = config.getString("regions." + id.toLowerCase(Locale.ROOT) + ".world");
        if (managedWorld != null && !managedWorld.equals(world.getName()))
        {
            throw new ProtectionException("managed-other-world", managedWorld);
        }

        List<ConfiguredFlag> flags = readPreset(preset);
        clearPreviouslyManagedFlags(region, id);
        applyFlags(region, flags);
        saveManagedRegion(world, region, preset, flags);
        return region;
    }

    public void remove(World world, String id)
    {
        RegionManager manager = requireRegionManager(world);
        if (!manager.hasRegion(id))
        {
            throw new ProtectionException("region-not-found", id);
        }
        manager.removeRegion(id);
        String path = "regions." + id.toLowerCase(Locale.ROOT);
        if (world.getName().equals(config.getString(path + ".world")))
        {
            config.set(path, null);
            config.save();
        }
    }

    public int reload()
    {
        config.load(false);
        return synchronizeConfiguredRegions();
    }

    private int synchronizeConfiguredRegions()
    {
        ConfigurationSection regions = config.getConfigurationSection("regions");
        if (regions == null)
        {
            return 0;
        }

        int loaded = 0;
        for (String id : regions.getKeys(false))
        {
            ConfigurationSection stored = regions.getConfigurationSection(id);
            if (stored == null)
            {
                continue;
            }
            try
            {
                World world = Bukkit.getWorld(Objects.requireNonNull(stored.getString("world"), "world is missing"));
                if (world == null)
                {
                    PlexLog.warn("Skipping protected region {0}: its world is not loaded.", id);
                    continue;
                }
                RegionManager manager = requireRegionManager(world);
                BlockVector3 minimum = readVector(stored, "minimum");
                BlockVector3 maximum = readVector(stored, "maximum");
                ProtectedRegion region = new ProtectedCuboidRegion(id, minimum, maximum);
                region.setPriority(stored.getInt("priority", 0));
                applyFlags(region, readFlags(stored.getConfigurationSection("flags"), "region " + id));
                manager.addRegion(region);
                loaded++;
            }
            catch (RuntimeException ex)
            {
                PlexLog.warn("Skipping protected region {0}: {1}", id, ex.getMessage());
            }
        }
        return loaded;
    }

    private List<ConfiguredFlag> readPreset(String preset)
    {
        ConfigurationSection section = config.getConfigurationSection("presets." + preset);
        if (section == null)
        {
            throw new ProtectionException("preset-not-found", preset);
        }
        return readFlags(section.getConfigurationSection("flags"), "preset " + preset);
    }

    private List<ConfiguredFlag> readFlags(@Nullable ConfigurationSection section, String source)
    {
        if (section == null)
        {
            return List.of();
        }

        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        List<ConfiguredFlag> flags = new ArrayList<>();
        for (String name : section.getKeys(false))
        {
            Flag<?> flag = registry.get(name);
            if (flag == null)
            {
                throw new ProtectionException("invalid-preset", source + " has unknown flag '" + name + "'");
            }

            Object configured = section.get(name);
            String groupName = null;
            Object rawValue = configured;
            if (configured instanceof ConfigurationSection valueSection)
            {
                if (valueSection.contains("value") || valueSection.contains("group"))
                {
                    rawValue = copyValue(valueSection.get("value"));
                    groupName = valueSection.getString("group");
                }
                else
                {
                    rawValue = copyValue(valueSection);
                }
            }

            Object value = unmarshal(flag, rawValue, source);
            RegionGroup group = parseGroup(flag, groupName, source);
            flags.add(new ConfiguredFlag(flag, value, group, copyValue(configured)));
        }
        return flags;
    }

    private void applyFlags(ProtectedRegion region, List<ConfiguredFlag> flags)
    {
        for (ConfiguredFlag configured : flags)
        {
            setFlag(region, configured.flag(), configured.value());
            if (configured.group() != null)
            {
                setFlag(region, configured.flag().getRegionGroupFlag(), configured.group());
            }
        }
    }

    private void clearPreviouslyManagedFlags(ProtectedRegion region, String id)
    {
        ConfigurationSection oldFlags = config.getConfigurationSection("regions." + id.toLowerCase(Locale.ROOT) + ".flags");
        if (oldFlags == null)
        {
            return;
        }
        FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
        for (String name : oldFlags.getKeys(false))
        {
            Flag<?> flag = registry.get(name);
            if (flag != null)
            {
                setFlag(region, flag, null);
                if (flag.getRegionGroupFlag() != null)
                {
                    setFlag(region, flag.getRegionGroupFlag(), null);
                }
            }
        }
    }

    private void saveManagedRegion(World world, ProtectedRegion region, String preset, List<ConfiguredFlag> flags)
    {
        String path = "regions." + region.getId().toLowerCase(Locale.ROOT);
        config.set(path + ".world", world.getName());
        config.set(path + ".minimum", vectorMap(region.getMinimumPoint()));
        config.set(path + ".maximum", vectorMap(region.getMaximumPoint()));
        config.set(path + ".priority", region.getPriority());
        config.set(path + ".preset", preset);

        Map<String, Object> storedFlags = new LinkedHashMap<>();
        for (ConfiguredFlag flag : flags)
        {
            storedFlags.put(flag.flag().getName(), flag.configuredValue());
        }
        config.set(path + ".flags", storedFlags);
        config.save();
    }

    private RegionManager requireRegionManager(World world)
    {
        RegionManager manager = getRegionManager(world);
        if (manager == null)
        {
            throw new ProtectionException("manager-unavailable", world.getName());
        }
        return manager;
    }

    private @Nullable RegionManager getRegionManager(World world)
    {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    private void validateRegionId(String id)
    {
        if (!ProtectedRegion.isValidId(id))
        {
            throw new ProtectionException("invalid-region-id", id);
        }
        String storedWorld = config.getString("regions." + id.toLowerCase(Locale.ROOT) + ".world");
        if (storedWorld != null)
        {
            throw new ProtectionException("region-exists", id);
        }
    }

    private static BlockVector3 readVector(ConfigurationSection section, String key)
    {
        if (!section.isInt(key + ".x") || !section.isInt(key + ".y") || !section.isInt(key + ".z"))
        {
            throw new IllegalArgumentException(key + " coordinates are missing");
        }
        return BlockVector3.at(section.getInt(key + ".x"), section.getInt(key + ".y"), section.getInt(key + ".z"));
    }

    private static Map<String, Object> vectorMap(BlockVector3 vector)
    {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("x", vector.x());
        values.put("y", vector.y());
        values.put("z", vector.z());
        return values;
    }

    private static Object copyValue(Object value)
    {
        if (value instanceof ConfigurationSection section)
        {
            Map<String, Object> values = new LinkedHashMap<>();
            for (String key : section.getKeys(false))
            {
                values.put(key, copyValue(section.get(key)));
            }
            return values;
        }
        if (value instanceof List<?> list)
        {
            return new ArrayList<>(list);
        }
        return value;
    }

    private static Object unmarshal(Flag<?> flag, Object rawValue, String source)
    {
        if (rawValue == null)
        {
            throw new ProtectionException("invalid-preset", source + " has no value for flag '" + flag.getName() + "'");
        }
        Object value;
        try
        {
            value = flag.unmarshal(rawValue);
        }
        catch (RuntimeException ex)
        {
            throw new ProtectionException("invalid-preset", source + " has an invalid value for flag '" + flag.getName() + "'");
        }
        if (value == null)
        {
            throw new ProtectionException("invalid-preset", source + " has an invalid value for flag '" + flag.getName() + "'");
        }
        return value;
    }

    private static @Nullable RegionGroup parseGroup(Flag<?> flag, @Nullable String groupName, String source)
    {
        if (groupName == null)
        {
            return null;
        }
        if (flag.getRegionGroupFlag() == null)
        {
            throw new ProtectionException("invalid-preset", source + " assigns a group to flag '" + flag.getName() + "', which does not support groups");
        }
        try
        {
            String normalized = groupName.replace('-', '_').toUpperCase(Locale.ROOT);
            normalized = switch (normalized)
            {
                case "NONMEMBERS" -> "NON_MEMBERS";
                case "NONOWNERS" -> "NON_OWNERS";
                default -> normalized;
            };
            return RegionGroup.valueOf(normalized);
        }
        catch (IllegalArgumentException ex)
        {
            throw new ProtectionException("invalid-preset", source + " has invalid group '" + groupName + "'");
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setFlag(ProtectedRegion region, Flag flag, Object value)
    {
        region.setFlag(flag, value);
    }

    private record ConfiguredFlag(Flag<?> flag, Object value, @Nullable RegionGroup group, Object configuredValue)
    {
    }

    public static final class ProtectionException extends RuntimeException
    {
        private final String reason;
        private final String detail;

        public ProtectionException(String reason, String detail)
        {
            super(reason + ": " + detail);
            this.reason = reason;
            this.detail = detail;
        }

        public String reason()
        {
            return reason;
        }

        public String detail()
        {
            return detail;
        }
    }
}
