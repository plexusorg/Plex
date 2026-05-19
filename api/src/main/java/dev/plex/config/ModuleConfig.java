package dev.plex.config;

import dev.plex.api.config.ModuleConfiguration;
import dev.plex.module.PlexModule;

/**
 * Public module config entry point. The platform installs a factory at runtime.
 */
public class ModuleConfig extends ModuleConfiguration
{
    private static Factory factory;
    private final ModuleConfiguration delegate;

    public static void setFactory(Factory factory)
    {
        ModuleConfig.factory = factory;
    }

    public ModuleConfig(PlexModule module, String from, String to)
    {
        if (factory == null)
        {
            throw new IllegalStateException("ModuleConfig factory has not been installed by Plex");
        }
        this.delegate = factory.create(module, from, to);
    }

    @Override
    public void load()
    {
        delegate.load();
    }

    @Override
    public void save()
    {
        delegate.save();
    }

    @Override
    public Object get(String path)
    {
        return delegate.get(path);
    }

    @Override
    public String getString(String path)
    {
        return delegate.getString(path);
    }

    @Override
    public String getString(String path, String def)
    {
        return delegate.getString(path, def);
    }

    @Override
    public int getInt(String path)
    {
        return delegate.getInt(path);
    }

    @Override
    public int getInt(String path, int def)
    {
        return delegate.getInt(path, def);
    }

    @Override
    public boolean getBoolean(String path)
    {
        return delegate.getBoolean(path);
    }

    @Override
    public void set(String path, Object value)
    {
        delegate.set(path, value);
    }

    @FunctionalInterface
    public interface Factory
    {
        ModuleConfiguration create(PlexModule module, String from, String to);
    }
}
