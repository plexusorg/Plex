package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.world.WorldSpawnSignManager;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;

public final class WorldSpawnSignListener extends ServerListenerBase
{
    private final WorldSpawnSignManager signManager;

    public WorldSpawnSignListener(Plex plugin)
    {
        super(plugin);
        this.signManager = plugin.getWorldSpawnSignManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBreak(BlockBreakEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockDamage(BlockDamageEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        cancelAndRestore(event.getBlockPlaced(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockBurn(BlockBurnEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFade(BlockFadeEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockForm(BlockFormEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockFlow(BlockFromToEvent event)
    {
        cancelAndRestore(event.getToBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockPhysics(BlockPhysicsEvent event)
    {
        if (signManager.isProtected(event.getBlock()) || signManager.isProtected(event.getSourceBlock()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        cancelAndRestore(event.getBlock(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSignChange(SignChangeEvent event)
    {
        if (signManager.isSign(event.getBlock()))
        {
            event.setCancelled(true);
            signManager.restore(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        Block block = event.getClickedBlock();
        if (block != null && signManager.isSign(block))
        {
            event.setCancelled(true);
            signManager.restore(block.getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onBlockExplode(BlockExplodeEvent event)
    {
        protectExplosion(event.blockList(), event.getBlock().getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        protectExplosion(event.blockList(), event.getLocation().getWorld());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonExtend(BlockPistonExtendEvent event)
    {
        if (movesProtectedBlock(event.getBlocks(), event.getDirection()))
        {
            event.setCancelled(true);
            signManager.restore(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPistonRetract(BlockPistonRetractEvent event)
    {
        if (movesProtectedBlock(event.getBlocks(), event.getDirection())
                || movesProtectedBlock(event.getBlocks(), event.getDirection().getOppositeFace()))
        {
            event.setCancelled(true);
            signManager.restore(event.getBlock().getWorld());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFertilize(BlockFertilizeEvent event)
    {
        protectBlockStates(event.getBlocks(), event.getBlock().getWorld(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onStructureGrow(StructureGrowEvent event)
    {
        protectBlockStates(event.getBlocks(), event.getLocation().getWorld(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpongeAbsorb(SpongeAbsorbEvent event)
    {
        protectBlockStates(event.getBlocks(), event.getBlock().getWorld(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPortalCreate(PortalCreateEvent event)
    {
        protectBlockStates(event.getBlocks(), event.getWorld(), event);
    }

    private void protectExplosion(List<Block> blocks, World world)
    {
        if (blocks.removeIf(signManager::isProtected))
        {
            signManager.restore(world);
        }
    }

    private boolean movesProtectedBlock(List<Block> blocks, org.bukkit.block.BlockFace direction)
    {
        return blocks.stream().anyMatch(block -> signManager.isProtected(block) || signManager.isProtected(block.getRelative(direction)));
    }

    private void protectBlockStates(List<BlockState> blocks, World world, org.bukkit.event.Cancellable event)
    {
        if (blocks.stream().map(BlockState::getBlock).anyMatch(signManager::isProtected))
        {
            event.setCancelled(true);
            signManager.restore(world);
        }
    }

    private void cancelAndRestore(Block block, org.bukkit.event.Cancellable event)
    {
        if (signManager.isProtected(block))
        {
            event.setCancelled(true);
            signManager.restore(block.getWorld());
        }
    }
}
