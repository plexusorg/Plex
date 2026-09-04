package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.api.message.MessageFormatter;
import dev.plex.api.message.MessagePlaceholder;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import dev.plex.command.PlexCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for Plex modules.
 *
 * <p>Use {@link #api()} for Plex services. Register native Paper tasks with
 * {@link #ownTask(ScheduledTask)} so Plex can cancel them when the module unloads.</p>
 */
public abstract class PlexModule
{
    private ModuleLifecycle lifecycle;
    private ModuleConfiguration messages;
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
     * Returns the Plex API.
     *
     * @return Plex API
     */
    public PlexApi api()
    {
        return requireLifecycle().api();
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
        requireLifecycle().registerListener(listener);
    }

    /** Returns the Paper plugin that owns this module's native scheduled tasks. */
    public Plugin plugin()
    {
        return requireLifecycle().plugin();
    }

    /** Registers a native Paper task for cancellation when this module unloads. */
    public <T extends ScheduledTask> @Nullable T ownTask(@Nullable T task)
    {
        return requireLifecycle().ownTask(task);
    }

    /**
     * Disconnects a player during module shutdown. The lifecycle owner schedules
     * the kick because this module's task scope is canceled immediately afterward.
     *
     * @param player player to disconnect
     * @param reason disconnect reason
     */
    public void kickPlayerOnShutdown(Player player, Component reason)
    {
        requireLifecycle().kickPlayerOnShutdown(player, reason);
    }

    /** Keeps this module's classloader open until its bounded asynchronous shutdown work finishes. */
    public void completeShutdownBeforeClose(CompletableFuture<Void> completion)
    {
        requireLifecycle().completeShutdownBeforeClose(completion);
    }

    /**
     * Unregisters and stops tracking a listener owned by this module.
     *
     * @param listener listener to unregister
     */
    public void unregisterListener(Listener listener)
    {
        requireLifecycle().unregisterListener(listener);
    }

    /**
     * Registers and tracks a command owned by this module.
     *
     * <p>Register commands in {@link #load()} to use them during the current
     * server run. Later changes usually require a server restart.</p>
     *
     * @param command command to register
     */
    public void registerCommand(PlexCommand command)
    {
        requireLifecycle().registerCommand(command);
    }

    /**
     * Unregisters and stops tracking a command owned by this module.
     *
     * <p>The command can remain active until Paper rebuilds its command list.
     * This usually happens after a server restart.</p>
     *
     * @param command command to unregister
     */
    public void unregisterCommand(PlexCommand command)
    {
        requireLifecycle().unregisterCommand(command);
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
        Objects.requireNonNull(name, "name");
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return requireLifecycle().commands().stream()
                .filter(command -> command.getName().equalsIgnoreCase(name) || command.getAliases().stream()
                        .map(alias -> alias.toLowerCase(Locale.ROOT))
                        .anyMatch(normalizedName::equals))
                .findFirst()
                .orElse(null);
    }

    /**
     * Opens a resource owned by this module.
     *
     * <p>Plex module class loaders delegate to Plex's class loader. This method
     * searches the module class loader itself first so common paths such as
     * {@code config.yml} cannot resolve to a resource bundled by Plex.</p>
     *
     * @param filename resource path
     * @return resource stream, or {@code null} when the resource cannot be opened
     */
    @Nullable
    public InputStream getResource(@NotNull String filename)
    {
        try
        {
            ClassLoader classLoader = this.getClass().getClassLoader();
            URL url = classLoader instanceof URLClassLoader moduleClassLoader
                    ? moduleClassLoader.findResource(filename)
                    : classLoader.getResource(filename);
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
     * Returns information read from this module's module.yml.
     *
     * @return module information
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
     * Loads this module's message file.
     *
     * @param fileName resource path and module data file path
     */
    public void loadMessages(String fileName)
    {
        messages = api().moduleConfigs().create(this, fileName);
        messages.load();
    }

    /**
     * Returns this module's loaded messages, if any.
     *
     * @return module messages, or {@code null} when this module has no messages
     */
    @Nullable
    public ModuleConfiguration messages()
    {
        return messages;
    }

    /**
     * Resolves a module message into a component, falling back to Plex messages.
     *
     * @param entry message key
     * @param placeholders named replacement values
     * @return resolved component
     */
    public Component messageComponent(String entry, MessagePlaceholder... placeholders)
    {
        String message = messages == null ? null : messages.getString(entry);
        if (message == null)
        {
            return api().messages().messageComponent(entry, placeholders);
        }
        return MessageFormatter.formatComponent(message, api().messages()::miniMessage, placeholders);
    }

    /**
     * Gets a module message as a component.
     *
     * @param entry message key
     * @return message component
     */
    public Component messageComponent(String entry)
    {
        return messageComponent(entry, new MessagePlaceholder[0]);
    }

    /**
     * Resolves a module message into a string, falling back to Plex messages.
     *
     * @param entry message key
     * @param placeholders named replacement values
     * @return resolved message string
     */
    public String messageString(String entry, MessagePlaceholder... placeholders)
    {
        String message = messages == null ? null : messages.getString(entry);
        if (message == null)
        {
            return api().messages().messageString(entry, placeholders);
        }
        return MessageFormatter.formatString(message, placeholders);
    }

    void setLifecycle(ModuleLifecycle lifecycle)
    {
        this.lifecycle = lifecycle;
    }

    void setPlexModuleFile(PlexModuleFile plexModuleFile)
    {
        this.plexModuleFile = plexModuleFile;
    }

    void setDataFolder(File dataFolder)
    {
        this.dataFolder = dataFolder;
    }

    void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    private ModuleLifecycle requireLifecycle()
    {
        if (lifecycle == null)
        {
            throw new IllegalStateException("Plex API is not available");
        }
        return lifecycle;
    }
}
