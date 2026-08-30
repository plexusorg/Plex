package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentManager;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.concurrent.CompletionException;

public class BanListener extends ServerListenerBase
{
    public BanListener(Plex plugin) { super(plugin); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        PunishmentManager.IndefiniteBan indefinite = plugin.getPunishmentManager().getIndefiniteBanByUUID(event.getUniqueId());
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "UUID");
            return;
        }

        final String ip = event.getAddress().getHostAddress();
        indefinite = plugin.getPunishmentManager().getIndefiniteBanByIP(ip);
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "IP");
            return;
        }
        indefinite = plugin.getPunishmentManager().getIndefiniteBanByUsername(event.getName());
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "username");
            return;
        }

        final Punishment punishment;
        try
        {
            punishment = plugin.getPunishmentManager().decideAdmission(event.getUniqueId(), ip).join().orElse(null);
        }
        catch (CompletionException failure)
        {
            PlexLog.error("Unable to evaluate admission for {0}: {1}", event.getUniqueId(), failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Unable to verify your ban status. Please try again shortly."));
            return;
        }

        if (punishment == null) return;
        if (plugin.getPermissions() != null && plugin.getPermissions().playerHas(null,
                Bukkit.getOfflinePlayer(event.getUniqueId()), "plex.ban.bypass")) return;
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                Punishment.generateAdmissionBanMessage(punishment, plugin.config.getString("banning.ban_url")));
    }

    private void disallowIndefinite(AsyncPlayerPreLoginEvent event, PunishmentManager.IndefiniteBan ban, String type)
    {
        String reason = ban.getReason();
        Component message = reason.isEmpty()
                ? Punishment.generateIndefBanMessage(type, plugin.config.getString("banning.ban_url"))
                : Punishment.generateIndefBanMessageWithReason(type, plugin.config.getString("banning.ban_url"), reason);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
    }
}
