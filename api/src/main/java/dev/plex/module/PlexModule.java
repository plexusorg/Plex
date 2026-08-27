package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.api.listener.EventRule;
import dev.plex.api.scheduler.TaskScope;
import dev.plex.command.PlexCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.Logger;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for Plex modules.
 *
 * <p>Use {@link #api()} for Plex services. Use {@link #scheduler()} for module
 * tasks.</p>
 */
public abstract class PlexModule
{
    private final Set<PlexCommand> commands = new LinkedHashSet<>();
    private final Set<Listener> listeners = new LinkedHashSet<>();

    private PlexApi api;
    private TaskScope scheduler;
    private ModuleConfiguration messages;
    private PlexModuleFile plexModuleFile;
    private File dataFolder;
    private File moduleJar;
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
        return requireApi();
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
        Objects.requireNonNull(listener, "listener");
        if (listeners.contains(listener))
        {
            throw new IllegalStateException("Listener is already registered");
        }
        requireApi().listeners().register(listener);
        listeners.add(listener);
    }

    /**
     * Returns this module's task scheduler.
     * Plex cancels its tasks when it unloads the module.
     *
     * @return module task scheduler
     */
    public TaskScope scheduler()
    {
        if (scheduler == null)
        {
            throw new IllegalStateException("Module task scheduler is not available");
        }
        return scheduler;
    }

    /**
     * Registers and tracks event rules owned by a listener.
     *
     * @param listener listener that owns the registrations
     * @param rules event rules to register
     */
    public void registerListener(Listener listener, EventRule<?>... rules)
    {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(rules, "rules");
        for (EventRule<?> rule : rules)
        {
            Objects.requireNonNull(rule, "rule");
        }
        if (listeners.contains(listener))
        {
            throw new IllegalStateException("Listener is already registered");
        }
        requireApi().listeners().register(listener);
        requireApi().listeners().register(listener, rules);
        listeners.add(listener);
    }

    /**
     * Registers event rules and tracks their listener owner.
     *
     * @param rules event rules to register
     * @return listener that owns the registrations
     */
    public Listener registerEventRules(EventRule<?>... rules)
    {
        Listener listener = requireApi().listeners().register(rules);
        listeners.add(listener);
        return listener;
    }

    /**
     * Unregisters and stops tracking a listener owned by this module.
     *
     * @param listener listener to unregister
     */
    public void unregisterListener(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        listeners.remove(listener);
        requireApi().listeners().unregister(listener);
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
        Objects.requireNonNull(command, "command");
        if (commands.contains(command))
        {
            throw new IllegalStateException("Command is already registered");
        }
        bindCommand(command);
        requireApi().commands().register(command);
        commands.add(command);
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
        Objects.requireNonNull(command, "command");
        commands.remove(command);
        requireApi().commands().unregister(command);
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
        return commands.stream()
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
     * Returns the commands tracked by this module.
     *
     * @return commands tracked by this module
     */
    public List<PlexCommand> getCommands()
    {
        return List.copyOf(commands);
    }

    /**
     * Returns the listeners tracked by this module.
     *
     * @return listeners tracked by this module
     */
    public List<Listener> getListeners()
    {
        return List.copyOf(listeners);
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
     * Returns the JAR file this module was loaded from.
     *
     * @return the JAR file this module was loaded from
     */
    public File getModuleJar()
    {
        return moduleJar;
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
     * @param from resource path to copy defaults from
     */
    public void loadMessages(String from)
    {
        loadMessages(from, "messages.yml");
    }

    /**
     * Loads this module's message file.
     *
     * @param from resource path to copy defaults from
     * @param to destination file path relative to the module data folder
     */
    public void loadMessages(String from, String to)
    {
        messages = requireApi().moduleConfigs().create(this, from, to);
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
     * @param objects replacement values
     * @return resolved component
     */
    public Component messageComponent(String entry, Object... objects)
    {
        return api.messages().miniMessage(messageString(entry, objects));
    }

    /**
     * Gets a module message as a component.
     *
     * @param entry message key
     * @return message component
     */
    public Component messageComponent(String entry)
    {
        return messageComponent(entry, new Object[0]);
    }

    /**
     * Resolves a module message into a component using component replacements.
     *
     * @param entry message key
     * @param objects component replacement values
     * @return resolved component
     */
    public Component messageComponent(String entry, Component... objects)
    {
        Component component = api.messages().miniMessage(messageString(entry));
        for (int i = 0; i < objects.length; i++)
        {
            int finalI = i;
            component = component.replaceText(builder -> builder.matchLiteral("{" + finalI + "}").replacement(objects[finalI]).build());
        }
        return component;
    }

    /**
     * Resolves a module message into a string, falling back to Plex messages.
     *
     * @param entry message key
     * @param objects replacement values
     * @return resolved message string
     */
    public String messageString(String entry, Object... objects)
    {
        String message = messages == null ? null : messages.getString(entry);
        if (message == null)
        {
            return api.messages().messageString(entry, objects);
        }
        for (int i = 0; i < objects.length; i++)
        {
            message = message.replace("{" + i + "}", String.valueOf(objects[i]));
        }
        return message;
    }

    void setApi(PlexApi api)
    {
        this.api = api;
        this.scheduler = api.scheduler().taskScope();
        commands.forEach(this::bindCommand);
    }

    private void bindCommand(PlexCommand command)
    {
        if (api != null)
        {
            command.bindApi(api);
        }
        command.bindModule(this);
    }

    void setPlexModuleFile(PlexModuleFile plexModuleFile)
    {
        this.plexModuleFile = plexModuleFile;
    }

    void setDataFolder(File dataFolder)
    {
        this.dataFolder = dataFolder;
    }

    void setModuleJar(File moduleJar)
    {
        this.moduleJar = moduleJar;
    }

    void setLogger(Logger logger)
    {
        this.logger = logger;
    }

    void cancelTasks()
    {
        if (scheduler != null)
        {
            scheduler.cancelAll();
        }
    }

    private PlexApi requireApi()
    {
        if (api == null)
        {
            throw new IllegalStateException("Plex API is not available");
        }
        return api;
    }
}
