package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentManager;
import dev.plex.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class BanListener extends ServerListenerBase
{
    public BanListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        final PunishmentManager.IndefiniteBan uuidBan = plugin.getPunishmentManager().getIndefiniteBanByUUID(event.getUniqueId());
        if (uuidBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !uuidBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("UUID", plugin.config.getString("banning.ban_url"), uuidBan.getReason()) : Punishment.generateIndefBanMessage("UUID", plugin.config.getString("banning.ban_url")));
            return;
        }

        final PunishmentManager.IndefiniteBan ipBan = plugin.getPunishmentManager().getIndefiniteBanByIP(event.getAddress().getHostAddress());
        if (ipBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !ipBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("IP", plugin.config.getString("banning.ban_url"), ipBan.getReason()) : Punishment.generateIndefBanMessage("IP", plugin.config.getString("banning.ban_url")));
            return;
        }

        final PunishmentManager.IndefiniteBan userBan = plugin.getPunishmentManager().getIndefiniteBanByUsername(event.getName());

        if (userBan != null)
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    !userBan.getReason().isEmpty() ? Punishment.generateIndefBanMessageWithReason("username", plugin.config.getString("banning.ban_url"), userBan.getReason()) : Punishment.generateIndefBanMessage("username", plugin.config.getString("banning.ban_url")));
            return;
        }

        if (plugin.getPunishmentManager().isBanned(event.getUniqueId()))
        {
            if (plugin.getPermissions() != null && plugin.getPermissions().playerHas(null, Bukkit.getOfflinePlayer(event.getUniqueId()), "plex.ban.bypass"))
            {
                return;
            }
            PlexPlayer player = plugin.getPlayerService().getPlayer(event.getUniqueId());
            player.getPunishments().stream().filter(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive()).findFirst().ifPresent(punishment ->
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url"), plugin.getPlayerNameResolver())));
            return;
        }
        Punishment ipBannedPunishment = plugin.getPunishmentManager().getBanByIP(event.getAddress().getHostAddress());
        if (ipBannedPunishment != null)
        {
            // Don't check if the other account that's banned has bypass abilities, check if current has only
            if (plugin.getPermissions() != null && plugin.getPermissions().playerHas(null, Bukkit.getOfflinePlayer(event.getUniqueId()), "plex.ban.bypass"))
            {
                return;
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    Punishment.generateBanMessage(ipBannedPunishment, plugin.config.getString("banning.ban_url"), plugin.getPlayerNameResolver()));
        }
    }
}
