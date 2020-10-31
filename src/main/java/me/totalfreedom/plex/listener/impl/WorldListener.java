package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;

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
                if (plexPlayer.getRankFromString().isAtleast(Rank.ADMIN))
                    return;
                e.setCancelled(true);
                player.sendMessage(PlexUtils.color(plugin.getMessageManager().getMessage("noAdminWorldBlockPlace")));
                break;
            }
        }
    }
}