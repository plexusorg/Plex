package dev.plex.api.config;

import org.bukkit.configuration.file.YamlConfiguration;

public abstract class ModuleConfiguration extends YamlConfiguration
{
    public abstract void load();
    public abstract void save();
}
