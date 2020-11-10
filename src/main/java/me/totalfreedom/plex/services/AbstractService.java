package me.totalfreedom.plex.services;

public abstract class AbstractService implements IService
{

    private boolean asynchronous;

    public AbstractService(boolean async)
    {
        this.asynchronous = async;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }
}
