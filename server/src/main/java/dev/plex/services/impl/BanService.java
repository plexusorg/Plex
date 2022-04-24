package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.services.AbstractService;
import dev.plex.util.TimeUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
        Plex.get().getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
        {
            punishments.forEach(punishment ->
            {
                if (ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE)).isAfter(punishment.getEndDate()))
                {
                    Plex.get().getPunishmentManager().unban(punishment);
                    Bukkit.broadcast(Component.text("Plex - Unbanned " + Bukkit.getOfflinePlayer(punishment.getPunished()).getName()));
                }
            });
        });
    }

    @Override
    public int repeatInSeconds()
    {
        // Every 30 seconds
        return 30;
    }
}
