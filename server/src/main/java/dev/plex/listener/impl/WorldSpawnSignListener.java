package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.world.WorldSpawnSignManager;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
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
        registerProtectionEvents();
    }

    private void registerProtectionEvents()
    {
        EventRule<?>[] rules = {
            blockRule(BlockBreakEvent.class, BlockBreakEvent::getBlock),
            blockRule(BlockDamageEvent.class, BlockDamageEvent::getBlock),
            blockRule(BlockPlaceEvent.class, BlockPlaceEvent::getBlockPlaced),
            blockRule(BlockBurnEvent.class, BlockBurnEvent::getBlock),
            blockRule(BlockFadeEvent.class, BlockFadeEvent::getBlock),
            blockRule(BlockFormEvent.class, BlockFormEvent::getBlock),
            blockRule(BlockFromToEvent.class, BlockFromToEvent::getToBlock),
            blockRule(EntityChangeBlockEvent.class, EntityChangeBlockEvent::getBlock),
            rule(BlockPhysicsEvent.class, event -> signManager.isProtected(event.getBlock()) || signManager.isProtected(event.getSourceBlock()), event -> event.getBlock().getWorld()),
            rule(SignChangeEvent.class, event -> signManager.isSign(event.getBlock()), event -> event.getBlock().getWorld()),
            rule(PlayerInteractEvent.class, event -> event.getClickedBlock() != null && signManager.isSign(event.getClickedBlock()), event -> event.getClickedBlock().getWorld()),
            blockStateRule(BlockFertilizeEvent.class, BlockFertilizeEvent::getBlocks, event -> event.getBlock().getWorld()),
            blockStateRule(StructureGrowEvent.class, StructureGrowEvent::getBlocks, event -> event.getLocation().getWorld()),
            blockStateRule(SpongeAbsorbEvent.class, SpongeAbsorbEvent::getBlocks, event -> event.getBlock().getWorld()),
            blockStateRule(PortalCreateEvent.class, PortalCreateEvent::getBlocks, PortalCreateEvent::getWorld),
        };
        plugin.getApi().listeners().register(this, rules);
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

    private <E extends Event & Cancellable> EventRule<E> blockRule(Class<E> eventType, Function<E, Block> block)
    {
        return rule(eventType, event -> signManager.isProtected(block.apply(event)), event -> block.apply(event).getWorld());
    }

    private <E extends Event & Cancellable> EventRule<E> blockStateRule(Class<E> eventType, Function<E, List<BlockState>> blocks, Function<E, World> world)
    {
        return rule(eventType, event -> blocks.apply(event).stream().map(BlockState::getBlock).anyMatch(signManager::isProtected), world);
    }

    private <E extends Event & Cancellable> EventRule<E> rule(Class<E> eventType, Predicate<E> blocked, Function<E, World> world)
    {
        return EventRule.blocking(eventType, EventPriority.HIGHEST, event ->
        {
            if (!blocked.test(event))
            {
                return false;
            }
            signManager.restore(world.apply(event));
            return true;
        });
    }
}
