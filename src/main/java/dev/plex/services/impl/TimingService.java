package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimingService extends AbstractService
{
    public static final Map<UUID, Long> cooldowns = new HashMap<>();

    public TimingService()
    {
        super(true, true);
    }

    @Override
    public void run()
    {
        cooldowns.clear();
    }

    @Override
    public int repeatInSeconds()
    {
        return 5;
    }
}
