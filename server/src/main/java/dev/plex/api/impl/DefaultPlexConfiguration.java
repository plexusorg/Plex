package dev.plex.api.impl;

import dev.plex.api.config.PlexConfiguration;
import dev.plex.config.Config;

import java.util.List;

final class DefaultPlexConfiguration implements PlexConfiguration
{
    private final Config config;

    DefaultPlexConfiguration(Config config)
    {
        this.config = config;
    }

    @Override
    public String getString(String path)
    {
        return config.getString(path);
    }

    @Override
    public boolean getBoolean(String path)
    {
        return config.getBoolean(path);
    }

    @Override
    public int getInt(String path)
    {
        return config.getInt(path);
    }

    @Override
    public List<String> getStringList(String path)
    {
        return config.getStringList(path);
    }

    @Override
    public void set(String path, Object value)
    {
        config.set(path, value);
    }

    @Override
    public void setComments(String path, List<String> comments)
    {
        config.setComments(path, comments);
    }

    @Override
    public void save()
    {
        config.save();
    }
}
