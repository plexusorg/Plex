package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;

import static dev.plex.util.PlexUtils.tl;

public class WorldListener extends PlexListener
{
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e)
    {
        Player player = e.getPlayer();
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
        World world = player.getWorld();
        switch (world.getName().toLowerCase())
        {
            case "adminworld" -> {
                if (plugin.getSystem().equalsIgnoreCase("ranks"))
                {
                    if (plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN))
                    {
                        return;
                    }
                } else if (plugin.getSystem().equalsIgnoreCase("permissions"))
                {
                    if (player.hasPermission("plex.adminworld"))
                    {
                        return;
                    }
                }
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
        {
            return;
        }
        if (e.getEntityType() != EntityType.SLIME)
        {
            return;
        }
        e.setCancelled(true);
    }
}