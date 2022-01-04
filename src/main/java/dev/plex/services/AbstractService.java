package dev.plex.services;

public abstract class AbstractService implements IService
{
    private boolean asynchronous;
    private boolean repeating;

    public AbstractService(boolean repeating, boolean async)
    {
        this.repeating = repeating;
        this.asynchronous = async;
    }

    public boolean isRepeating() {
        return repeating;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }
}
