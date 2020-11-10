package me.totalfreedom.plex.services.impl;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.banning.Ban;
import me.totalfreedom.plex.services.AbstractService;
import org.bukkit.Bukkit;

import java.util.Date;

public class BanService extends AbstractService
{
    public BanService() {
        super(true);
    }

    @Override
    public void run() {
        for (Ban ban : Plex.get().getBanManager().getActiveBans())
        {
            if (new Date().after(ban.getEndDate()))
            {
                Plex.get().getBanManager().unban(ban.getId());
                Bukkit.broadcastMessage("Plex - Unbanned " + Bukkit.getOfflinePlayer(ban.getUuid()).getName());
            }
        }
    }

    @Override
    public int repeatInSeconds() {
        return 10;
    }
}
