package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.cache.MongoPlayerData;
import dev.plex.cache.PlayerCache;
import dev.plex.cache.SQLPlayerData;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Collections;
import java.util.UUID;

public class PlayerListener extends PlexListener
{
    private final MongoPlayerData mongoPlayerData = plugin.getMongoPlayerData() != null ? plugin.getMongoPlayerData() : null;
    private final SQLPlayerData sqlPlayerData = plugin.getSqlPlayerData() != null ? plugin.getSqlPlayerData() : null;

    // setting up a player's data
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSetup(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        PlexPlayer plexPlayer;

        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            player.setOp(true);
            PlexLog.debug("Automatically opped " + player.getName() + " since ranks are enabled.");
        } else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            player.setOp(false);
            PlexLog.debug("Automatically deopped " + player.getName() + " since ranks are disabled.");
        }

        if (!DataUtils.hasPlayedBefore(player.getUniqueId()))
        {
            PlexLog.log("A player with this name has not joined the server before, creating new entry.");
            plexPlayer = new PlexPlayer(player.getUniqueId()); //it doesn't! okay so now create the object
            plexPlayer.setName(player.getName()); //set the name of the player
            plexPlayer.setIps(Collections.singletonList(player.getAddress().getAddress().getHostAddress().trim())); //set the arraylist of ips
            DataUtils.insert(plexPlayer); // insert data in some wack db
        } else
        {
            plexPlayer = DataUtils.getPlayer(player.getUniqueId());
        }

        PunishedPlayer punishedPlayer;
        PlayerCache.getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
        if (!PlayerCache.getPunishedPlayerMap().containsKey(player.getUniqueId()))
        {
            punishedPlayer = new PunishedPlayer(player.getUniqueId());
            PlayerCache.getPunishedPlayerMap().put(player.getUniqueId(), punishedPlayer);
        } else
        {
            punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        }
        punishedPlayer.convertPunishments();

        assert plexPlayer != null;

        String loginMessage = plugin.getRankManager().getLoginMessage(plexPlayer);

        if (!loginMessage.isEmpty())
        {
            event.joinMessage(
                    Component.text(ChatColor.AQUA + player.getName() + " is ").color(NamedTextColor.AQUA).append(LegacyComponentSerializer.legacyAmpersand().deserialize(loginMessage))
                    .append(Component.newline())
                    .append(Component.text(player.getName() +  " joined the game").color(NamedTextColor.YELLOW))
            );
        }
    }

    // saving the player's data
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSave(PlayerQuitEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId()); //get the player because it's literally impossible for them to not have an object

        if (plugin.getRankManager().isAdmin(plexPlayer))
        {
            plugin.getAdminList().removeFromCache(UUID.fromString(plexPlayer.getUuid()));
        }

        if (mongoPlayerData != null) //back to mongo checking
        {
            mongoPlayerData.update(plexPlayer); //update the player's document
        } else if (sqlPlayerData != null) //sql checking
        {
            sqlPlayerData.update(plexPlayer);
        }

        PlayerCache.getPlexPlayerMap().remove(event.getPlayer().getUniqueId()); //remove them from cache
    }
}
