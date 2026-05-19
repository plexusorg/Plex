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

    /**
     * Installs the platform factory used to create module configurations.
     *
     * @param factory module configuration factory
     */
    public static void setFactory(Factory factory)
    {
        ModuleConfig.factory = factory;
    }

    /**
     * Creates or opens a module configuration through the installed platform factory.
     *
     * @param module module that owns the configuration
     * @param from resource path to copy defaults from
     * @param to destination file path relative to the module data folder
     * @throws IllegalStateException if Plex has not installed a factory yet
     */
    public ModuleConfig(PlexModule module, String from, String to)
    {
        if (factory == null)
        {
            throw new IllegalStateException("ModuleConfig factory has not been installed by Plex");
        }
        this.delegate = factory.create(module, from, to);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load()
    {
        delegate.load();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void save()
    {
        delegate.save();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(String path)
    {
        return delegate.get(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object get(String path, Object def)
    {
        return delegate.get(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String path)
    {
        return delegate.getString(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString(String path, String def)
    {
        return delegate.getString(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(String path)
    {
        return delegate.getInt(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt(String path, int def)
    {
        return delegate.getInt(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(String path)
    {
        return delegate.getBoolean(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean(String path, boolean def)
    {
        return delegate.getBoolean(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(String path)
    {
        return delegate.getLong(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong(String path, long def)
    {
        return delegate.getLong(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(String path)
    {
        return delegate.getDouble(path);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble(String path, double def)
    {
        return delegate.getDouble(path, def);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void set(String path, Object value)
    {
        delegate.set(path, value);
    }

    /**
     * Factory installed by the platform to create module configuration delegates.
     */
    @FunctionalInterface
    public interface Factory
    {
        /**
         * Creates or opens a module configuration.
         *
         * @param module module that owns the configuration
         * @param from resource path to copy defaults from
         * @param to destination file path relative to the module data folder
         * @return created configuration
         */
        ModuleConfiguration create(PlexModule module, String from, String to);
    }
}
