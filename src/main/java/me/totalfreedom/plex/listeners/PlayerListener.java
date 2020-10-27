package me.totalfreedom.plex.listeners;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.MongoPlayerData;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        if (mongoPlayerData != null) // Alright, check if we're saving with Mongo first
        {
            if (!mongoPlayerData.exists(player.getUniqueId())) //okay, we're saving with mongo! now check if the player's document exists
            {
                PlexLog.log("AYO THIS MAN DONT EXIST"); // funi msg
                PlexPlayer plexPlayer = new PlexPlayer(player.getUniqueId()); //it doesn't! okay so now create the object
                plexPlayer.setName(player.getName()); //set the name of the player
                plexPlayer.setIps(Arrays.asList(player.getAddress().getAddress().getHostAddress().trim())); //set the arraylist of ips

                Plex.get().getMongoPlayerData().getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
                Plex.get().getMongoPlayerData().getPunishedPlayerMap().put(player.getUniqueId(), new PunishedPlayer(player.getUniqueId()));

                Plex.get().getMongoPlayerData().getPlexPlayerDAO().save(plexPlayer); //and put their document in mongo collection

            } else {
                PlexPlayer plexPlayer = Plex.get().getMongoPlayerData().getByUUID(player.getUniqueId()); //oh they do exist!
                plexPlayer.setName(plexPlayer.getName()); //set the name!
                Plex.get().getMongoPlayerData().getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //cache them!
                Plex.get().getMongoPlayerData().getPunishedPlayerMap().put(player.getUniqueId(), new PunishedPlayer(player.getUniqueId()));
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event)
    {
        if (mongoPlayerData != null) //back to mongo checking
        {
            PlexPlayer plexPlayer = Plex.get().getMongoPlayerData().getPlexPlayerMap().get(event.getPlayer().getUniqueId()); //get the player because it's literally impossible for them to not have an object
            Plex.get().getMongoPlayerData().update(plexPlayer); //update the player's document

            Plex.get().getMongoPlayerData().getPlexPlayerMap().remove(event.getPlayer().getUniqueId()); //remove them from cache
            Plex.get().getMongoPlayerData().getPunishedPlayerMap().remove(event.getPlayer().getUniqueId());
        }

    }

}
