package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentManager;
import dev.plex.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanListener extends PlexListener
{
    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final PunishmentManager.IndefiniteBan uuidBan = plugin.getPunishmentManager().getIndefiniteBanByUUID(event.getUniqueId());
        if (uuidBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !uuidBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("UUID", uuidBan.getReason()) : Punishment.generateIndefBanMessage("UUID"));
            return;
        }

        final PunishmentManager.IndefiniteBan ipBan = plugin.getPunishmentManager().getIndefiniteBanByIP(event.getAddress().getHostAddress());
        if (ipBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !ipBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("IP", ipBan.getReason()) : Punishment.generateIndefBanMessage("IP"));
            return;
        }

        final PunishmentManager.IndefiniteBan userBan = plugin.getPunishmentManager().getIndefiniteBanByUsername(event.getName());

        if (userBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !userBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("username", userBan.getReason()) : Punishment.generateIndefBanMessage("username"));
            return;
        }

        if (plugin.getPunishmentManager().isBanned(event.getUniqueId()))
        {
            if (Plex.get().getPermissions() != null && Plex.get().getPermissions().playerHas(null, Bukkit.getOfflinePlayer(event.getUniqueId()), "plex.ban.bypass")) return;
            PlexPlayer player = DataUtils.getPlayer(event.getUniqueId());
            player.getPunishments().stream().filter(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive()).findFirst().ifPresent(punishment ->
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            Punishment.generateBanMessage(punishment)));
        }
    }
}