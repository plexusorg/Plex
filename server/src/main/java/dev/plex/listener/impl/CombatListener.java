package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.List;

public class CombatListener extends PlexListener
{
    public List<String> blockedPlayers = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player target && event.getDamager() instanceof Player attacker) {
            if (blockedPlayers.contains(target.getName()) || blockedPlayers.contains(attacker.getName())) {
                event.setCancelled(true);
            }
        }
    }
}
