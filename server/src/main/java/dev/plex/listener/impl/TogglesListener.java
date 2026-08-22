package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.CommandUtils;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;

import org.bukkit.Bukkit;
import org.bukkit.ExplosionResult;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.potion.PotionEffectTypeCategory;

public class TogglesListener extends ServerListenerBase
{
    public TogglesListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event)
    {
        if (!plugin.toggles.getBoolean("explosions") && event.getExplosionResult() != ExplosionResult.TRIGGER_BLOCK)
        {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        if (!plugin.toggles.getBoolean("explosions") && event.getExplosionResult() != ExplosionResult.TRIGGER_BLOCK)
        {
            event.blockList().clear();
        }
    }

    @EventHandler
    public void onFluidSpread(BlockFromToEvent event)
    {
        if (!plugin.toggles.getBoolean("fluidspread") && event.getBlock().isLiquid())
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (!plugin.toggles.getBoolean("drops"))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        Player player = event.getPlayer();
        if (!plugin.toggles.getBoolean("chat") && !plugin.getPermissions().has(player, "plex.mute.bypass"))
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("chatIsOff"));
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        if (!plugin.toggles.getBoolean("chat") && !plugin.getPermissions().has(player, "plex.mute.bypass"))
        {
            if (CommandUtils.matchesCommand(plugin, event.getMessage(), plugin.config.getStringList("block_on_mute")))
            {
                event.getPlayer().sendMessage(PlexUtils.messageComponent("chatIsOff"));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(PrePlayerAttackEntityEvent event)
    {
        if (!plugin.toggles.getBoolean("pvp") && event.willAttack() && event.getAttacked() instanceof Player)
        {
            event.setCancelled(true);
            event.getPlayer().sendMessage(PlexUtils.messageComponent("pvpDisabled"));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event)
    {
        if (plugin.toggles.getBoolean("pvp") || !(event.getEntity() instanceof Player victim))
        {
            return;
        }

        Player attacker = responsiblePlayer(event.getDamageSource().getCausingEntity());
        if (attacker == null && event instanceof EntityDamageByEntityEvent entityDamage)
        {
            attacker = responsiblePlayer(entityDamage.getDamager());
        }
        cancelPvp(event, attacker, victim);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPotionEffect(EntityPotionEffectEvent event)
    {
        if (plugin.toggles.getBoolean("pvp") || !(event.getEntity() instanceof Player victim) ||
                event.getNewEffect() == null ||
                event.getNewEffect().getType().getCategory() != PotionEffectTypeCategory.HARMFUL)
        {
            return;
        }

        Player attacker = responsiblePlayer(event.getSource());
        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerCombust(EntityCombustByEntityEvent event)
    {
        if (plugin.toggles.getBoolean("pvp") || !(event.getEntity() instanceof Player victim))
        {
            return;
        }

        Player attacker = responsiblePlayer(event.getCombuster());
        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerKnockback(EntityPushedByEntityAttackEvent event)
    {
        if (plugin.toggles.getBoolean("pvp") || !(event.getEntity() instanceof Player victim))
        {
            return;
        }

        Player attacker = responsiblePlayer(event.getPushedBy());
        if (attacker != null && !attacker.getUniqueId().equals(victim.getUniqueId()))
        {
            event.setCancelled(true);
        }
    }

    private Player responsiblePlayer(Entity source)
    {
        if (source instanceof Player player)
        {
            return player;
        }
        if (source instanceof Projectile projectile)
        {
            return responsiblePlayer(projectile.getShooter());
        }
        if (source instanceof AreaEffectCloud cloud)
        {
            return responsiblePlayer(cloud.getSource());
        }
        if (source instanceof TNTPrimed tnt)
        {
            return responsiblePlayer(tnt.getSource());
        }
        if (source instanceof Tameable tameable && tameable.getOwnerUniqueId() != null)
        {
            return Bukkit.getPlayer(tameable.getOwnerUniqueId());
        }
        return null;
    }

    private Player responsiblePlayer(ProjectileSource source)
    {
        return source instanceof Entity entity ? responsiblePlayer(entity) : null;
    }

    private void cancelPvp(EntityDamageEvent event, Player attacker, Player victim)
    {
        if (attacker == null || attacker.getUniqueId().equals(victim.getUniqueId()))
        {
            return;
        }

        event.setCancelled(true);
        attacker.sendMessage(PlexUtils.messageComponent("pvpDisabled"));
    }

    /* I have no idea if this is the best way to do this
    There is a very weird bug where if you try to create a loop using two repeaters and a lever, after disabling
    and re-enabling redstone, you are unable to recreate the loop with a lever. Using a redstone torch works fine.
    Using a lever works fine also as long as you never toggle redstone.
     */
    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event)
    {
        if (!plugin.toggles.getBoolean("redstone"))
        {
            event.setNewCurrent(0);
        }
    }
}
