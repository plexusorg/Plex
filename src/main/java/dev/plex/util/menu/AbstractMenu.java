package dev.plex.util.menu;

import dev.plex.Plex;
import org.bukkit.event.Listener;

public abstract class AbstractMenu implements Listener
{
    private final String name;

    public AbstractMenu(String name)
    {
        this.name = name;

        Plex.get().getServer().getPluginManager().registerEvents(this, Plex.get());
    }

    public String getName()
    {
        return name;
    }
}
