package dev.plex.module;

import dev.plex.api.PlexApi;

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
import org.bukkit.command.Command;
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
    private final List<Command> commands = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    private PlexApi api;
    private PlexModuleFile plexModuleFile;
    private File dataFolder;
    private Logger logger;

    public PlexApi api()
    {
        return api;
    }

    public void load()
    {
    }

    public void enable()
    {
    }

    public void disable()
    {
    }

    public void registerListener(Listener listener)
    {
        listeners.add(listener);
    }

    public void unregisterListener(Listener listener)
    {
        listeners.remove(listener);
        HandlerList.unregisterAll(listener);
    }

    public void registerCommand(Command command)
    {
        commands.add(command);
    }

    public void unregisterCommand(Command command)
    {
        commands.remove(command);
    }

    @Nullable
    public Command getCommand(String name)
    {
        return commands.stream()
                .filter(command -> command.getName().equalsIgnoreCase(name) || command.getAliases().stream().map(String::toLowerCase).toList().contains(name.toLowerCase(Locale.ROOT)))
                .findFirst()
                .orElse(null);
    }

    public void addDefaultMessage(String message, Object initValue)
    {
        if (api.configuration().messages().getString(message) == null)
        {
            api.configuration().messages().set(message, initValue);
            api.configuration().messages().save();
            logger.debug("'{}' message added from {}", message, plexModuleFile.getName());
        }
    }

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

    public List<Command> getCommands()
    {
        return commands;
    }

    public List<Listener> getListeners()
    {
        return listeners;
    }

    public PlexModuleFile getPlexModuleFile()
    {
        return plexModuleFile;
    }

    public File getDataFolder()
    {
        return dataFolder;
    }

    public Logger getLogger()
    {
        return logger;
    }

    public void setApi(PlexApi api)
    {
        this.api = api;
    }

    public void setPlexModuleFile(PlexModuleFile plexModuleFile)
    {
        this.plexModuleFile = plexModuleFile;
    }

    public void setDataFolder(File dataFolder)
    {
        this.dataFolder = dataFolder;
    }

    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
