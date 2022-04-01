package dev.plex.module;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.listener.PlexListener;
import java.io.File;
import java.util.List;
import java.util.Locale;

//import dev.plex.module.loader.CustomClassLoader;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.Logger;
import org.bukkit.event.HandlerList;

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

    public void registerListener(PlexListener listener)
    {
        listeners.add(listener);
    }

    public void unregisterListener(PlexListener listener)
    {
        listeners.remove(listener);
        HandlerList.unregisterAll(listener);
    }

    public void registerCommand(PlexCommand command)
    {
        commands.add(command);
    }

    public void unregisterCommand(PlexCommand command)
    {
        commands.remove(command);
    }

    public PlexCommand getCommand(String name)
    {
        return commands.stream().filter(plexCommand -> plexCommand.getName().equalsIgnoreCase(name) || plexCommand.getAliases().stream().map(String::toLowerCase).toList().contains(name.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
    }
}
