package dev.plex.module;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexLog;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
@Setter(AccessLevel.MODULE)
public abstract class PlexModule
{
    @Getter(AccessLevel.MODULE)
    private final List<PlexCommand> commands = Lists.newArrayList();

    @Getter(AccessLevel.MODULE)
    private final List<PlexListener> listeners = Lists.newArrayList();

    private Plex plex;
    private PlexModuleFile plexModuleFile;
    private File dataFolder;
    private Logger logger;

    public void load()
    {
    }

    public void enable()
    {
    }

    public void disable()
    {
    }

    /**
     * Registers a PlexListener within a module
     *
     * @param listener The PlexListener to be registered
     */
    public void registerListener(PlexListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Unregisters a PlexListener. Handled by Plex automatically
     *
     * @param listener The PlexListener to be registered
     */
    public void unregisterListener(PlexListener listener)
    {
        listeners.remove(listener);
        HandlerList.unregisterAll(listener);
    }

    /**
     * Registers a PlexCommand within a module
     *
     * @param command The PlexCommand to be registered
     */
    public void registerCommand(PlexCommand command)
    {
        commands.add(command);
    }

    /**
     * Unregisters a PlexCommand. Handled by Plex automatically
     *
     * @param command The PlexCommand to be registered
     */
    public void unregisterCommand(PlexCommand command)
    {
        commands.remove(command);
    }

    public PlexCommand getCommand(String name)
    {
        return commands.stream().filter(plexCommand -> plexCommand.getName().equalsIgnoreCase(name) || plexCommand.getAliases().stream().map(String::toLowerCase).toList().contains(name.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
    }

    /**
     * Adds a message to the messages.yml file
     *
     * @param message   The key value for the message
     * @param initValue The message itself
     */
    public void addDefaultMessage(String message, Object initValue)
    {
        if (plex.messages.getString(message) == null)
        {
            plex.messages.set(message, initValue);
            plex.messages.save();
            PlexLog.debug("'{0}' message added from " + plexModuleFile.getName(), message);
        }
    }

    /**
     * Adds a message to the messages.yml with a comment
     *
     * @param message   The key value for the message
     * @param initValue The message itself
     * @param comments  The comments to be placed above the message
     */
    public void addDefaultMessage(String message, Object initValue, String... comments)
    {
        if (plex.messages.getString(message) == null)
        {
            plex.messages.set(message, initValue);
            plex.messages.save();
            plex.messages.setComments(message, Arrays.asList(comments));
            plex.messages.save();
            PlexLog.debug("'{0}' message added from " + plexModuleFile.getName(), message);
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
}
