package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import static me.totalfreedom.plex.util.PlexUtils.tl;

public class WorldListener extends PlexListener
{
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e)
    {
        Player player = e.getPlayer();
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
        World world = player.getWorld();
        switch (world.getName())
        {
            case "adminworld":
            {
                if (plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN))
                    return;
                e.setCancelled(true);
                player.sendMessage(tl("noAdminWorldBlockPlace"));
                break;
            }
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e)
    {
        if (!e.getLocation().getWorld().getName().equals("fionn"))
            return;
        if (e.getEntityType() != EntityType.SLIME)
            return;
        e.setCancelled(true);
    }
}