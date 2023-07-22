package dev.plex.services;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public interface IService
{
    void run(ScheduledTask scheduledTask);

    int repeatInSeconds();
}
