package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import io.papermc.paper.event.block.PlayerShearBlockEvent;
import io.papermc.paper.event.entity.EntityCompostItemEvent;
import io.papermc.paper.event.entity.EntityDyeEvent;
import io.papermc.paper.event.player.PlayerChangeBeaconEffectEvent;
import io.papermc.paper.event.player.PlayerFlowerPotManipulateEvent;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import io.papermc.paper.event.player.PlayerLecternPageChangeEvent;
import io.papermc.paper.event.player.PlayerNameEntityEvent;
import io.papermc.paper.event.player.PlayerOpenSignEvent;
import io.papermc.paper.event.player.PlayerToggleEntityAgeLockEvent;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CauldronLevelChangeEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerHarvestBlockEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerUnleashEntityEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;

public class WorldListener extends ServerListenerBase
{
    private static final Set<String> EDIT_COMMANDS = Set.of("bigtree", "ebigtree", "largetree", "elargetree", "break", "ebreak", "antioch", "nuke", "editsign", "tree", "etree");

    public WorldListener(Plex plugin)
    {
        super(plugin);
        registerModificationEvents();
        registerEntryEvents();
    }

    private void registerModificationEvents()
    {
        EventRule<?>[] rules = {
            rule(BlockPlaceEvent.class, BlockPlaceEvent::getPlayer, event -> event.getBlockPlaced().getWorld()),
            rule(BlockBreakEvent.class, BlockBreakEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerInteractEvent.class, PlayerInteractEvent::getPlayer, event -> event.getClickedBlock() == null ? event.getPlayer().getWorld() : event.getClickedBlock().getWorld()),
            rule(PlayerInteractEntityEvent.class, PlayerInteractEntityEvent::getPlayer, event -> event.getRightClicked().getWorld()),
            rule(PlayerInteractAtEntityEvent.class, PlayerInteractAtEntityEvent::getPlayer, event -> event.getRightClicked().getWorld()),
            rule(PlayerArmorStandManipulateEvent.class, PlayerArmorStandManipulateEvent::getPlayer, event -> event.getRightClicked().getWorld()),
            rule(EntityDamageByEntityEvent.class, event -> responsiblePlayer(event.getDamageSource().getCausingEntity(), event.getDamager()), event -> event.getEntity().getWorld()),
            rule(EntityPlaceEvent.class, EntityPlaceEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(HangingPlaceEvent.class, HangingPlaceEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(HangingBreakByEntityEvent.class, HangingBreakByEntityEvent::getRemover, event -> event.getEntity().getWorld()),
            rule(VehicleDestroyEvent.class, VehicleDestroyEvent::getAttacker, event -> event.getVehicle().getWorld()),
            rule(VehicleDamageEvent.class, event -> responsiblePlayer(event.getDamageSource().getCausingEntity(), event.getAttacker()), event -> event.getVehicle().getWorld()),
            rule(ProjectileLaunchEvent.class, ProjectileLaunchEvent::getEntity, event -> event.getEntity().getWorld()),
            rule(BlockIgniteEvent.class, BlockIgniteEvent::getIgnitingEntity, event -> event.getBlock().getWorld()),
            rule(EntityChangeBlockEvent.class, EntityChangeBlockEvent::getEntity, event -> event.getBlock().getWorld()),
            rule(EntityInteractEvent.class, EntityInteractEvent::getEntity, event -> event.getBlock().getWorld()),
            rule(EntityCompostItemEvent.class, EntityCompostItemEvent::getEntity, event -> event.getBlock().getWorld()),
            rule(CauldronLevelChangeEvent.class, CauldronLevelChangeEvent::getEntity, event -> event.getBlock().getWorld()),
            rule(PlayerBucketEmptyEvent.class, PlayerBucketEmptyEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerBucketFillEvent.class, PlayerBucketFillEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerBucketEntityEvent.class, PlayerBucketEntityEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(BlockFertilizeEvent.class, BlockFertilizeEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(StructureGrowEvent.class, StructureGrowEvent::getPlayer, StructureGrowEvent::getWorld),
            rule(PlayerHarvestBlockEvent.class, PlayerHarvestBlockEvent::getPlayer, event -> event.getHarvestedBlock().getWorld()),
            rule(PlayerFlowerPotManipulateEvent.class, PlayerFlowerPotManipulateEvent::getPlayer, event -> event.getFlowerpot().getWorld()),
            rule(SignChangeEvent.class, SignChangeEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerTakeLecternBookEvent.class, PlayerTakeLecternBookEvent::getPlayer, event -> event.getLectern().getWorld()),
            rule(PlayerInsertLecternBookEvent.class, PlayerInsertLecternBookEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerLecternPageChangeEvent.class, PlayerLecternPageChangeEvent::getPlayer, event -> event.getLectern().getWorld()),
            rule(PlayerOpenSignEvent.class, PlayerOpenSignEvent::getPlayer, event -> event.getSign().getWorld()),
            rule(PlayerChangeBeaconEffectEvent.class, PlayerChangeBeaconEffectEvent::getPlayer, event -> event.getBeacon().getWorld()),
            rule(PlayerItemFrameChangeEvent.class, PlayerItemFrameChangeEvent::getPlayer, event -> event.getItemFrame().getWorld()),
            rule(PlayerNameEntityEvent.class, PlayerNameEntityEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(PlayerToggleEntityAgeLockEvent.class, PlayerToggleEntityAgeLockEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(PlayerShearBlockEvent.class, PlayerShearBlockEvent::getPlayer, event -> event.getBlock().getWorld()),
            rule(PlayerBedEnterEvent.class, PlayerBedEnterEvent::getPlayer, event -> event.getBed().getWorld()),
            rule(PlayerShearEntityEvent.class, PlayerShearEntityEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(PlayerLeashEntityEvent.class, PlayerLeashEntityEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(PlayerUnleashEntityEvent.class, PlayerUnleashEntityEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(EntityTameEvent.class, event -> event.getOwner() instanceof Player player ? player : null, event -> event.getEntity().getWorld()),
            rule(EntityDyeEvent.class, EntityDyeEvent::getPlayer, event -> event.getEntity().getWorld()),
            rule(EntityEnterLoveModeEvent.class, EntityEnterLoveModeEvent::getHumanEntity, event -> event.getEntity().getWorld()),
            rule(InventoryOpenEvent.class, InventoryOpenEvent::getPlayer, event -> inventoryWorld(event.getView())),
            rule(InventoryClickEvent.class, InventoryClickEvent::getWhoClicked, event -> inventoryWorld(event.getView())),
            rule(InventoryDragEvent.class, InventoryDragEvent::getWhoClicked, event -> inventoryWorld(event.getView())),
        };
        plugin.getApi().listeners().register(this, rules);
    }

    private void registerEntryEvents()
    {
        EventRule<?>[] rules = {
            EventRule.blocking(PlayerTeleportEvent.class, EventPriority.HIGHEST, event -> event.getTo() != null && !canEnterWorld(event.getPlayer(), event.getTo().getWorld(), true)),
            EventRule.of(EntityTeleportEvent.class, EventPriority.HIGHEST, this::onEntityTeleport),
            EventRule.blocking(EntityMountEvent.class, EventPriority.HIGHEST, event -> event.getEntity() instanceof Player player && !canEnterWorld(player, event.getMount().getWorld(), true)),
            EventRule.of(PlayerJoinEvent.class, EventPriority.HIGHEST, event -> enforceEntry(event.getPlayer(), null)),
            EventRule.of(PlayerChangedWorldEvent.class, EventPriority.HIGHEST, event -> enforceEntry(event.getPlayer(), event.getFrom())),
            EventRule.of(PlayerRespawnEvent.class, EventPriority.HIGHEST, this::onPlayerRespawn),
        };
        plugin.getApi().listeners().register(this, rules);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event)
    {
        Player player = responsiblePlayer(event.getEntity());
        if (player != null && denyModification(player, event.getLocation().getWorld()))
        {
            event.blockList().clear();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        if (canModifyWorld(player, player.getWorld()))
        {
            return;
        }

        String label = event.getMessage().replaceFirst("^/", "").replaceFirst("\\s.*", "").toLowerCase(Locale.ROOT);
        String baseLabel = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        Command command = Bukkit.getCommandMap().getCommand(label);
        if (command == null)
        {
            command = Bukkit.getCommandMap().getCommand(baseLabel);
        }

        if (EDIT_COMMANDS.contains(baseLabel) || isWorldEditCommand(command))
        {
            sendModificationMessage(player, player.getWorld());
            event.setCancelled(true);
        }
    }

    private void onEntityTeleport(EntityTeleportEvent event)
    {
        Location destination = event.getTo();
        if (destination == null)
        {
            return;
        }
        if (!canPassengersEnter(event.getEntity(), destination.getWorld()))
        {
            event.setCancelled(true);
        }
    }

    private void onPlayerRespawn(PlayerRespawnEvent event)
    {
        Player player = event.getPlayer();
        if (canEnterWorld(player, event.getRespawnLocation().getWorld(), true))
        {
            return;
        }
        Location fallback = findAllowedSpawn(player, null);
        if (fallback != null)
        {
            event.setRespawnLocation(fallback);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.getEntityType() == EntityType.SLIME)
        {
            event.setCancelled(true);
        }
    }

    private <E extends Event & Cancellable> EventRule<E> rule(Class<E> eventType, Function<E, ? extends Entity> actor, Function<E, World> world)
    {
        return EventRule.blocking(eventType, EventPriority.HIGHEST, event ->
        {
            Player player = responsiblePlayer(actor.apply(event));
            World affectedWorld = world.apply(event);
            return player != null && affectedWorld != null && denyModification(player, affectedWorld);
        });
    }

    private World inventoryWorld(InventoryView view)
    {
        Location location = view.getTopInventory().getLocation();
        return location == null ? null : location.getWorld();
    }

    private boolean denyModification(Player player, World world)
    {
        if (canModifyWorld(player, world))
        {
            return false;
        }
        sendModificationMessage(player, world);
        return true;
    }

    private boolean canModifyWorld(Player player, World world)
    {
        String key = worldKey(world);
        if (key == null)
        {
            return true;
        }
        String permission = plugin.worlds.getString("worlds." + key + ".modification.permission");
        return permission == null || player.hasPermission(permission);
    }

    private void sendModificationMessage(Player player, World world)
    {
        String key = worldKey(world);
        if (key == null)
        {
            return;
        }
        String message = plugin.worlds.getString("worlds." + key + ".modification.message");
        if (message != null && !message.isBlank())
        {
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    private boolean canEnterWorld(Player player, World world, boolean showMessage)
    {
        String key = worldKey(world);
        if (key == null)
        {
            return true;
        }
        String permission = plugin.worlds.getString("worlds." + key + ".entry.permission");
        if (permission == null || player.hasPermission(permission))
        {
            return true;
        }
        if (showMessage)
        {
            String message = plugin.worlds.getString("worlds." + key + ".entry.message");
            if (message != null && !message.isBlank())
            {
                player.sendMessage(MiniMessage.miniMessage().deserialize(message));
            }
        }
        return false;
    }

    private String worldKey(World world)
    {
        ConfigurationSection worlds = plugin.worlds.getConfigurationSection("worlds");
        if (worlds == null)
        {
            return null;
        }
        return worlds.getKeys(false).stream().filter(key -> key.equalsIgnoreCase(world.getName())).findFirst().orElse(null);
    }

    private void enforceEntry(Player player, World preferredFallback)
    {
        if (canEnterWorld(player, player.getWorld(), true))
        {
            return;
        }
        Location fallback = findAllowedSpawn(player, preferredFallback);
        if (fallback != null)
        {
            player.teleportAsync(fallback);
        }
    }

    private Location findAllowedSpawn(Player player, World preferred)
    {
        if (preferred != null && canEnterWorld(player, preferred, false))
        {
            return preferred.getSpawnLocation();
        }
        return Bukkit.getWorlds().stream()
                .filter(world -> !world.equals(player.getWorld()))
                .filter(world -> canEnterWorld(player, world, false))
                .map(World::getSpawnLocation)
                .findFirst()
                .orElse(null);
    }

    private boolean isWorldEditCommand(Command command)
    {
        if (!(command instanceof PluginIdentifiableCommand identifiable))
        {
            return false;
        }
        Plugin owner = identifiable.getPlugin();
        return owner.getName().equalsIgnoreCase("WorldEdit") || owner.getName().equalsIgnoreCase("FastAsyncWorldEdit");
    }

    private boolean canPassengersEnter(Entity entity, World world)
    {
        for (Entity passenger : entity.getPassengers())
        {
            if (passenger instanceof Player player && !canEnterWorld(player, world, true))
            {
                return false;
            }
            if (!canPassengersEnter(passenger, world))
            {
                return false;
            }
        }
        return true;
    }

    private Player responsiblePlayer(Entity source)
    {
        if (source == null)
        {
            return null;
        }
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
        for (Entity passenger : source.getPassengers())
        {
            Player player = responsiblePlayer(passenger);
            if (player != null)
            {
                return player;
            }
        }
        if (source instanceof Tameable tameable && tameable.getOwnerUniqueId() != null)
        {
            return Bukkit.getPlayer(tameable.getOwnerUniqueId());
        }
        return null;
    }

    private Player responsiblePlayer(Entity... sources)
    {
        for (Entity source : sources)
        {
            Player player = responsiblePlayer(source);
            if (player != null)
            {
                return player;
            }
        }
        return null;
    }

    private Player responsiblePlayer(ProjectileSource source)
    {
        return source instanceof Entity entity ? responsiblePlayer(entity) : null;
    }
}
