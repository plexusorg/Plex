package dev.plex.listener.impl;

import dev.plex.banning.Ban;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
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
        if (plugin.getBanManager().isBanned(event.getUniqueId()))
        {
            for (Ban ban : plugin.getBanManager().getActiveBans())
            {
                PlexPlayer player = DataUtils.getPlayer(ban.getBanner());
                String banMessage = PlexUtils.tl("banMessage", banUrl, ban.getReason(),
                        DATE_FORMAT.format(ban.getEndDate()), ban.getBanner() == null ? "CONSOLE" : player.getName());
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                        LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage));
            }
        }
    }
}