package dev.plex.services.impl;

import dev.plex.cache.DataUtils;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.services.AbstractService;
import dev.plex.util.TimeUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class TimingService extends AbstractService
{
    public static final Map<UUID, Long> spamCooldown = new HashMap<>();
    public static final Map<UUID, Long> nukerCooldown = new HashMap<>();
    // 1 - Default
    // 2 - Kick
    // 3 - Tempban 5 minutes
    // 4 - Ban
    public static final Map<UUID, Long> strikes = new HashMap<>();

    public TimingService()
    {
        super(true, true);
    }

    @Override
    public void run()
    {
        spamCooldown.clear();
        nukerCooldown.clear();
        for (Map.Entry<UUID, Long> map : strikes.entrySet())
        {
            if (map.getValue() == 2)
            {
                Player player = Bukkit.getPlayer(map.getKey());
                if (player != null)
                {
                    player.kick(Component.text("Please disable your nuker!").color(NamedTextColor.RED));
                }
            }
            if (map.getValue() == 3)
            {
                issueBan(map, "5m");
            }
            else if (map.getValue() == 4)
            {
                issueBan(map, "24h");
                map.setValue(0L);
            }
        }
    }

    private void issueBan(Map.Entry<UUID, Long> map, String time)
    {
        Punishment punishment = new Punishment(map.getKey(), null);
        Player player = Bukkit.getPlayer(map.getKey());
        PlexPlayer plexPlayer = DataUtils.getPlayer(map.getKey());
        punishment.setType(PunishmentType.TEMPBAN);
        punishment.setReason("You are temporarily banned for five minutes for either using a Nuker or spamming chat/commands.");
        if (player != null)
        {
            punishment.setPunishedUsername(player.getName());
            punishment.setIp(player.getAddress().getAddress().getHostAddress());
        }
        punishment.setEndDate(TimeUtils.createDate(time));
        punishment.setCustomTime(false);
        punishment.setActive(!plugin.getRankManager().isAdmin(plexPlayer));
        plugin.getPunishmentManager().punish(plexPlayer, punishment);
        if (player != null)
        {
            player.kick(Punishment.generateBanMessage(punishment));
        }
    }

    @Override
    public int repeatInSeconds()
    {
        return 5;
    }
}
