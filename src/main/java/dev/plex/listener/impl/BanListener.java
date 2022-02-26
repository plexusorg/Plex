package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanListener extends PlexListener
{
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (plugin.getPunishmentManager().isBanned(event.getUniqueId()))
        {
            PunishedPlayer player = PlayerCache.getPunishedPlayer(event.getUniqueId());
            player.getPunishments().stream().filter(punishment -> punishment.getType() == PunishmentType.BAN && punishment.isActive()).findFirst().ifPresent(punishment ->
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            Punishment.generateBanMessage(punishment)));
        }
    }
}