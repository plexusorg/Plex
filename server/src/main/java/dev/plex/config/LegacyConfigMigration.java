package dev.plex.config;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import java.io.File;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;

public final class LegacyConfigMigration
{
    private static final List<String> ENTITY_ROOTS = List.of(
            "entitywipe_list", "autowipe", "blocked_blocks", "blocked_entities", "entity_limit");
    private static final List<String> WORLD_ROOTS = List.of("global_gamerules", "worlds");
    private static final Map<String, String> RENAMED_PATHS = Map.ofEntries(
            Map.entry("data.central.storage", "data.db.storage"),
            Map.entry("data.central.user", "data.db.user"),
            Map.entry("data.central.password", "data.db.password"),
            Map.entry("data.central.hostname", "data.db.hostname"),
            Map.entry("data.central.port", "data.db.port"),
            Map.entry("data.central.db", "data.db.name"),
            Map.entry("data.side.enabled", "data.redis.enabled"),
            Map.entry("data.side.auth", "data.redis.auth"),
            Map.entry("data.side.hostname", "data.redis.hostname"),
            Map.entry("data.side.port", "data.redis.port"),
            Map.entry("data.side.password", "data.redis.password"));

    private final boolean migrateEntityConfig;
    private final boolean migrateWorldConfig;

    public LegacyConfigMigration(Plex plugin)
    {
        migrateEntityConfig = !new File(plugin.getDataFolder(), "entities.yml").exists();
        migrateWorldConfig = !new File(plugin.getDataFolder(), "worlds.yml").exists();
    }

    public void migrate(Config config, Config entities, Config worlds)
    {
        config.load(false);
        entities.load(false);
        worlds.load(false);

        boolean configChanged = migrateRenamedPaths(config);
        configChanged |= migrateColors(config);
        configChanged |= migrateSplitConfig(config, entities, ENTITY_ROOTS, migrateEntityConfig,
                "Moved entity settings from config.yml to entities.yml.");
        configChanged |= migrateSplitConfig(config, worlds, WORLD_ROOTS, migrateWorldConfig,
                "Moved world settings from config.yml to worlds.yml.");
        if (configChanged)
        {
            config.save();
        }
    }

    private boolean migrateRenamedPaths(Config config)
    {
        boolean changed = false;
        for (Map.Entry<String, String> rename : RENAMED_PATHS.entrySet())
        {
            if (!config.contains(rename.getKey()))
            {
                continue;
            }
            if (!config.contains(rename.getValue()))
            {
                config.set(rename.getValue(), config.get(rename.getKey()));
            }
            config.set(rename.getKey(), null);
            changed = true;
        }
        removeEmptySection(config, "data.central");
        removeEmptySection(config, "data.side");
        if (changed)
        {
            PlexLog.log("Renamed legacy database and Redis settings in config.yml.");
        }
        return changed;
    }

    private boolean migrateColors(Config config)
    {
        ConfigurationSection colors = config.getConfigurationSection("colors");
        if (colors == null)
        {
            return false;
        }
        for (String group : colors.getKeys(false))
        {
            String target = "groups." + group + ".color";
            if (!config.contains(target))
            {
                config.set(target, colors.get(group));
            }
        }
        config.set("colors", null);
        PlexLog.log("Moved legacy group colors to groups in config.yml.");
        return true;
    }

    private boolean migrateSplitConfig(Config source, Config target, List<String> roots, boolean enabled,
                                       String successMessage)
    {
        if (!enabled || !migrateRoots(source, target, roots))
        {
            return false;
        }
        target.save();
        PlexLog.log(successMessage);
        return true;
    }

    private boolean migrateRoots(Config source, Config target, List<String> roots)
    {
        boolean changed = false;
        for (String root : roots)
        {
            if (!source.contains(root))
            {
                continue;
            }
            ConfigurationSection section = source.getConfigurationSection(root);
            if (section == null)
            {
                target.set(root, source.get(root));
            }
            else
            {
                for (String path : section.getKeys(true))
                {
                    if (!section.isConfigurationSection(path))
                    {
                        target.set(root + "." + path, section.get(path));
                    }
                }
            }
            source.set(root, null);
            changed = true;
        }
        return changed;
    }

    private void removeEmptySection(Config config, String path)
    {
        ConfigurationSection section = config.getConfigurationSection(path);
        if (section != null && section.getKeys(false).isEmpty())
        {
            config.set(path, null);
        }
    }
}
