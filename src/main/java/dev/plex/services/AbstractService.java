package dev.plex.services;

import dev.plex.PlexBase;

public abstract class AbstractService extends PlexBase implements IService
{
    private boolean asynchronous;
    private boolean repeating;

    public AbstractService(boolean repeating, boolean async)
    {
        this.repeating = repeating;
        this.asynchronous = async;
    }

    public boolean isRepeating()
    {
        return repeating;
    }

    public boolean isAsynchronous()
    {
        return asynchronous;
    }
}
