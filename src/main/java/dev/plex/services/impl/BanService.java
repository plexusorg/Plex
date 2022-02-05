package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.banning.Ban;
import dev.plex.services.AbstractService;

import java.time.LocalDateTime;
import java.util.Date;
import org.bukkit.Bukkit;

public class BanService extends AbstractService
{
    public BanService()
    {
        super(true, true);
    }

    @Override
    public void run()
    {
        for (Ban ban : Plex.get().getBanManager().getActiveBans())
        {
            if (LocalDateTime.now().isAfter(ban.getEndDate()))
            {
                Plex.get().getBanManager().unban(ban.getId());
                Bukkit.broadcastMessage("Plex - Unbanned " + Bukkit.getOfflinePlayer(ban.getUuid()).getName());
            }
        }
    }

    @Override
    public int repeatInSeconds()
    {
        return 10;
    }
}
