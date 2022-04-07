package dev.plex.services;

import dev.plex.PlexBase;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AbstractService extends PlexBase implements IService
{
    private boolean asynchronous;
    private boolean repeating;

    @Setter
    private int taskId;

    public AbstractService(boolean repeating, boolean async)
    {
        this.repeating = repeating;
        this.asynchronous = async;
    }
}
