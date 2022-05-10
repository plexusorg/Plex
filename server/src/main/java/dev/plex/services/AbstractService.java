package dev.plex.services;

import dev.plex.PlexBase;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AbstractService implements IService, PlexBase
{
    private final boolean asynchronous;
    private final boolean repeating;

    @Setter
    private int taskId;

    public AbstractService(boolean repeating, boolean async)
    {
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
