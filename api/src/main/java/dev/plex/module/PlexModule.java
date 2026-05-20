package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.command.PlexCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.Logger;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for Plex modules.
 *
 * <p>This class is part of the public module API. Modules use {@link #api()} for
 * supported integration points.</p>
 */
public abstract class PlexModule
{
    private final List<PlexCommand> commands = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    private PlexApi api;
    private PlexModuleFile plexModuleFile;
    private File dataFolder;
    private Logger logger;

    /**
     * Creates a Plex module.
     */
    public PlexModule()
    {
    }

    /**
     * Returns the Plex API facade.
     *
     * @return Plex API facade for supported module integrations
     */
    public PlexApi api()
    {
        return api;
    }

    /**
     * Called when the module is loaded.
     */
    public void load()
    {
    }

    /**
     * Called when the module is enabled.
     */
    public void enable()
    {
    }

    /**
     * Called when the module is disabled.
     */
    public void disable()
    {
    }

    /**
     * Registers and tracks a listener owned by this module.
     *
     * @param listener listener to register
     */
    public void registerListener(Listener listener)
    {
        listeners.add(listener);
        if (api != null)
        {
            api.listeners().register(listener);
        }
    }

    /**
     * Unregisters and stops tracking a listener owned by this module.
     *
     * @param listener listener to unregister
     */
    public void unregisterListener(Listener listener)
    {
        listeners.remove(listener);
        if (api != null)
        {
            api.listeners().unregister(listener);
            return;
        }
        HandlerList.unregisterAll(listener);
    }

    /**
     * Registers and tracks a command owned by this module.
     *
     * <p>Paper Brigadier commands are lifecycle-registered. Commands registered
     * during module load before Plex's command handler initializes are active for
     * the current startup. Commands registered after the Paper command lifecycle
     * has already run are tracked by Plex but are not guaranteed to appear in the
     * live dispatcher until Paper rebuilds lifecycle commands, normally on a full
     * server restart.</p>
     *
     * @param command command to register
     */
    public void registerCommand(PlexCommand command)
    {
        commands.add(command);
        if (api != null)
        {
            api.commands().register(command);
        }
    }

    /**
     * Unregisters and stops tracking a command owned by this module.
     *
     * <p>Unregistration removes the command from this module and Plex's registry.
     * If Paper has already built the active Brigadier dispatcher, the command may
     * remain callable until Paper rebuilds lifecycle commands.</p>
     *
     * @param command command to unregister
     */
    public void unregisterCommand(PlexCommand command)
    {
        commands.remove(command);
        if (api != null)
        {
            api.commands().unregister(command);
        }
    }

    /**
     * Looks up a tracked command by name or alias.
     *
     * @param name command name or alias
     * @return matching command, or {@code null} when no command matches
     */
    @Nullable
    public PlexCommand getCommand(String name)
    {
        return commands.stream()
                .filter(command -> command.getName().equalsIgnoreCase(name) || command.getAliases().stream().map(String::toLowerCase).toList().contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    /**
     * Adds a default message if the message key is not already configured.
     *
     * @param message message key
     * @param initValue default value to write
     */
    public void addDefaultMessage(String message, Object initValue)
    {
        if (api.configuration().messages().getString(message) == null)
        {
            api.configuration().messages().set(message, initValue);
            api.configuration().messages().save();
            logger.debug("'{}' message added from {}", message, plexModuleFile.getName());
        }
    }

    /**
     * Adds a default message and comments if the message key is not already configured.
     *
     * @param message message key
     * @param initValue default value to write
     * @param comments comments to write above the message key
     */
    public void addDefaultMessage(String message, Object initValue, String... comments)
    {
        if (api.configuration().messages().getString(message) == null)
        {
            api.configuration().messages().set(message, initValue);
            api.configuration().messages().save();
            api.configuration().messages().setComments(message, Arrays.asList(comments));
            api.configuration().messages().save();
            logger.debug("'{}' message added from {}", message, plexModuleFile.getName());
        }
    }

    /**
     * Opens a resource from this module's class loader.
     *
     * @param filename resource path
     * @return resource stream, or {@code null} when the resource cannot be opened
     */
    @Nullable
    public InputStream getResource(@NotNull String filename)
    {
        try
        {
            URL url = this.getClass().getClassLoader().getResource(filename);
            if (url == null)
            {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    /**
     * Returns commands currently tracked by this module.
     *
     * @return commands currently tracked by this module
     */
    public List<PlexCommand> getCommands()
    {
        return commands;
    }

    /**
     * Returns listeners currently tracked by this module.
     *
     * @return listeners currently tracked by this module
     */
    public List<Listener> getListeners()
    {
        return listeners;
    }

    /**
     * Returns metadata read from this module's module.yml.
     *
     * @return metadata read from this module's module.yml
     */
    public PlexModuleFile getPlexModuleFile()
    {
        return plexModuleFile;
    }

    /**
     * Returns the module data folder.
     *
     * @return module data folder
     */
    public File getDataFolder()
    {
        return dataFolder;
    }

    /**
     * Returns the module logger.
     *
     * @return module logger
     */
    public Logger getLogger()
    {
        return logger;
    }

    /**
     * Sets the Plex API facade for this module.
     *
     * @param api Plex API facade
     */
    public void setApi(PlexApi api)
    {
        this.api = api;
    }

    /**
     * Sets metadata read from this module's module.yml.
     *
     * @param plexModuleFile module metadata
     */
    public void setPlexModuleFile(PlexModuleFile plexModuleFile)
    {
        this.plexModuleFile = plexModuleFile;
    }

    /**
     * Sets the module data folder.
     *
     * @param dataFolder data folder
     */
    public void setDataFolder(File dataFolder)
    {
        this.dataFolder = dataFolder;
    }

    /**
     * Sets the module logger.
     *
     * @param logger logger
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
