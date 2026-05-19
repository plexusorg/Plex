package dev.plex.api.impl;

import dev.plex.api.config.PlexConfiguration;
import dev.plex.config.TomlConfig;

import java.util.List;

final class DefaultPlexConfiguration implements PlexConfiguration
{
    private final TomlConfig config;

    DefaultPlexConfiguration(TomlConfig config)
    {
        this.config = config;
    }

    @Override
    public String getString(String path)
    {
        return config.getToml().getString(path);
    }

    @Override
    public boolean getBoolean(String path)
    {
        return config.getToml().getBoolean(path, false);
    }

    @Override
    public int getInt(String path)
    {
        Long value = config.getToml().getLong(path, 0L);
        return value.intValue();
    }

    @Override
    public List<String> getStringList(String path)
    {
        return config.getToml().getList(path, List.of());
    }

    @Override
    public void set(String path, Object value)
    {
        throw new UnsupportedOperationException("Proxy TOML configuration writes are not supported through PlexConfiguration");
    }

    @Override
    public void setComments(String path, List<String> comments)
    {
        throw new UnsupportedOperationException("Proxy TOML configuration comments are not supported through PlexConfiguration");
    }

    @Override
    public void save()
    {
        throw new UnsupportedOperationException("Proxy TOML configuration saves are not supported through PlexConfiguration");
    }
}
