package me.totalfreedom.plex.listeners;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.MongoPlayerData;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.cache.SQLPlayerData;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.player.PunishedPlayer;
import me.totalfreedom.plex.util.PlexLog;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Arrays;

public class PlayerListener implements Listener
{

    private MongoPlayerData mongoPlayerData = Plex.get().getMongoPlayerData() != null ? Plex.get().getMongoPlayerData() : null;
    private SQLPlayerData sqlPlayerData = Plex.get().getSqlPlayerData() != null ? Plex.get().getSqlPlayerData() : null;

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();

        PlexPlayer plexPlayer = null;

        if (mongoPlayerData != null) // Alright, check if we're saving with Mongo first
        {
            if (!mongoPlayerData.exists(player.getUniqueId())) //okay, we're saving with mongo! now check if the player's document exists
            {
                PlexLog.log("AYO THIS MAN DONT EXIST"); // funi msg
                plexPlayer = new PlexPlayer(player.getUniqueId()); //it doesn't! okay so now create the object
                plexPlayer.setName(player.getName()); //set the name of the player
                plexPlayer.setIps(Arrays.asList(player.getAddress().getAddress().getHostAddress().trim())); //set the arraylist of ips

                mongoPlayerData.getPlexPlayerDAO().save(plexPlayer); //and put their document in mongo collection

            } else {
                plexPlayer = mongoPlayerData.getByUUID(player.getUniqueId()); //oh they do exist!
                plexPlayer.setName(plexPlayer.getName()); //set the name!
            }
        } else if (sqlPlayerData != null)
        {
            if (!sqlPlayerData.exists(player.getUniqueId())) //okay, we're saving with mongo! now check if the player's document exists
            {
                PlexLog.log("AYO THIS MAN DONT EXIST"); // funi msg
                plexPlayer = new PlexPlayer(player.getUniqueId()); //it doesn't! okay so now create the object
                plexPlayer.setName(player.getName()); //set the name of the player
                plexPlayer.setIps(Arrays.asList(player.getAddress().getAddress().getHostAddress().trim())); //set the arraylist of ips

                sqlPlayerData.insert(plexPlayer); //and put their document in mongo collection

            } else {
                plexPlayer = sqlPlayerData.getByUUID(player.getUniqueId()); //oh they do exist!
                plexPlayer.setName(plexPlayer.getName()); //set the name!
            }
        }

        PlayerCache.getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
        PlayerCache.getPunishedPlayerMap().put(player.getUniqueId(), new PunishedPlayer(player.getUniqueId()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId()); //get the player because it's literally impossible for them to not have an object

        if (mongoPlayerData != null) //back to mongo checking
        {
            mongoPlayerData.update(plexPlayer); //update the player's document
        } else if (sqlPlayerData != null)
        {
            sqlPlayerData.update(plexPlayer);
        }

        PlayerCache.getPlexPlayerMap().remove(event.getPlayer().getUniqueId()); //remove them from cache
        PlayerCache.getPunishedPlayerMap().remove(event.getPlayer().getUniqueId());

    }

}
