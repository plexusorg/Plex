package dev.plex.services;

import dev.plex.Plex;
import lombok.Getter;

@Getter
public abstract class AbstractService implements IService
{
    protected final Plex plugin;
    private final boolean asynchronous;
    private final boolean repeating;

    public AbstractService(Plex plugin, boolean repeating, boolean async)
    {
        this.plugin = plugin;
        this.repeating = repeating;
        this.asynchronous = async;
    }

    public void onStart()
    {
    }

    public void onEnd()
    {
    }
}
