package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.MojangUtils;
import dev.plex.util.PlexUtils;
import java.time.format.DateTimeFormatter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanListener extends PlexListener
{
    private final String banUrl = plugin.config.getString("banning.ban_url");
    private final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a");

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (plugin.getPunishmentManager().isBanned(event.getUniqueId()))
        {
            PunishedPlayer player = PlayerCache.getPunishedPlayer(event.getUniqueId());
            player.getPunishments().stream().filter(punishment -> punishment.getType() == PunishmentType.BAN && punishment.isActive()).findFirst().ifPresent(punishment ->
            {
                String banMessage = PlexUtils.tl("banMessage", banUrl, punishment.getReason(),
                        DATE_FORMAT.format(punishment.getEndDate()), punishment.getPunisher() == null ? "CONSOLE" : MojangUtils.getInfo(punishment.getPunisher().toString()).getUsername());
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage));
            });
        }
    }
}