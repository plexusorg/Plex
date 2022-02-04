package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener extends PlexListener
{
    private final String banUrl = plugin.config.getString("banning.ban_url");

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (plugin.getBanManager().isBanned(event.getUniqueId()))
        {
            PunishedPlayer punishedPlayer = new PunishedPlayer(event.getUniqueId());
            Punishment punishment = punishedPlayer.getPunishments().get(punishedPlayer.getPunishments().size() - 1);
            String banMessage;
            if (punishment.getPunisher() == null)
            {
                banMessage = PlexUtils.tl("banMessage", banUrl, punishment.getReason(),
                        punishment.getEndDate(), "CONSOLE");
            }
            else
            {
                PlexPlayer player = DataUtils.getPlayer(punishment.getPunisher());
                banMessage = PlexUtils.tl("banMessage", banUrl, punishment.getReason(),
                        punishment.getEndDate(), player.getName());
            }
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage));
        }
    }
}
