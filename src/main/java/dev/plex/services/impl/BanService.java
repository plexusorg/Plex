package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.punishment.Punishment;
import dev.plex.services.AbstractService;
import java.time.LocalDateTime;
import net.kyori.adventure.text.Component;
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
        for (Punishment punishment : Plex.get().getPunishmentManager().getActiveBans())
        {
            if (LocalDateTime.now().isAfter(punishment.getEndDate()))
            {
                Plex.get().getPunishmentManager().unban(punishment);
                Bukkit.broadcast(Component.text("Plex - Unbanned " + Bukkit.getOfflinePlayer(punishment.getPunished()).getName()));
            }
        }
    }

    @Override
    public int repeatInSeconds()
    {
        // Every 5 minutes
        return 300;
    }
}
