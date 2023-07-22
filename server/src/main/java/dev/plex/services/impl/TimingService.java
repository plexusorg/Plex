package dev.plex.services.impl;

import dev.plex.cache.DataUtils;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TimingService extends AbstractService
{
    public static final Map<UUID, Long> spamCooldown = new HashMap<>();
    public static final Map<UUID, Long> nukerCooldown = new HashMap<>();
    public static final Map<UUID, Long> strikes = new HashMap<>();

    public TimingService()
    {
        super(true, true);
    }

    @Override
    public void run(ScheduledTask task)
    {
        spamCooldown.clear();
        nukerCooldown.clear();
        for (Map.Entry<UUID, Long> map : strikes.entrySet())
        {
            PlexLog.debug(map.getKey() + ": " + map.getValue());
            // Tempban for 5 minutes and reset strikes. This will probably stop people from actually trying to use a Nuker to grief.
            if (map.getValue() >= 2L)
            {
                issueBan(map);
                strikes.remove(map.getKey());
            }
        }
    }

    private void issueBan(Map.Entry<UUID, Long> map)
    {
        Punishment punishment = new Punishment(map.getKey(), null);
        Player player = Bukkit.getPlayer(map.getKey());
        PlexPlayer plexPlayer = DataUtils.getPlayer(map.getKey());
        punishment.setType(PunishmentType.TEMPBAN);
        punishment.setReason("You are temporarily banned for five minutes for using a Nuker.");
        if (player != null)
        {
            punishment.setPunishedUsername(player.getName());
            punishment.setIp(player.getAddress().getAddress().getHostAddress());
        }
        punishment.setEndDate(TimeUtils.createDate("5m"));
        punishment.setCustomTime(false);
        punishment.setActive(!plexPlayer.isAdminActive());
        plugin.getPunishmentManager().punish(plexPlayer, punishment);
    }

    @Override
    public int repeatInSeconds()
    {
        return 5;
    }
}
