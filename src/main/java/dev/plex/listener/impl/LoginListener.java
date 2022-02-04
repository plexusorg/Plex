package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener extends PlexListener
{
    private final String banMessage = plugin.config.getString("banning.message");

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (plugin.getBanManager().isBanned(event.getUniqueId()))
        {
            PunishedPlayer player = new PunishedPlayer(event.getUniqueId());
            Punishment punishment = player.getPunishments().get(player.getPunishments().size() - 1);
            PlexLog.debug("This player is banned. Outputting information:");
            PlexLog.debug("UUID: " + player.getUuid());
            PlexLog.debug("Username: " + punishment.getPunishedUsername());
            PlexLog.debug("Punisher: " + punishment.getPunisher());
            PlexLog.debug("Reason: " + punishment.getReason());
            PlexLog.debug("End date: " + punishment.getEndDate());
            PlexLog.debug("IPs: " + punishment.getIPS());
            PlexLog.debug("Type: " + punishment.getType());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage));
        }
    }
}
