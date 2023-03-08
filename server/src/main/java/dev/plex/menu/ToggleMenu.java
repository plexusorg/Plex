package dev.plex.menu;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.util.PlexUtils;
import dev.plex.util.menu.AbstractMenu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ToggleMenu extends AbstractMenu implements PlexBase
{
    private final List<Inventory> inventories = Lists.newArrayList();

    public ToggleMenu()
    {
        super("§a§lToggles");
        Inventory inventory = Bukkit.createInventory(null, 9, PlexUtils.mmDeserialize("Toggles"));
        resetExplosionItem(inventory);
        resetFluidspreadItem(inventory);
        resetDropsItem(inventory);
        resetRedstoneItem(inventory);
        inventories.add(inventory);
    }

    public void openInv(Player player, int index)
    {
        player.openInventory(inventories.get(index));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (event.getClickedInventory() == null)
        {
            return;
        }
        Inventory inv = event.getClickedInventory();
        if (!isValidInventory(inv))
        {
            return;
        }
        if (event.getCurrentItem() == null)
        {
            return;
        }
        if (!event.getCurrentItem().hasItemMeta())
        {
            return;
        }
        if (!event.getCurrentItem().getItemMeta().hasDisplayName())
        {
            return;
        }
        ItemStack item = event.getCurrentItem();
        event.setCancelled(true);
        if (item.getType() == Material.TNT)
        {
            plugin.toggles.set("explosions", !plugin.toggles.getBoolean("explosions"));
            resetExplosionItem(inv);
            event.getWhoClicked().sendMessage(PlexUtils.mmDeserialize("<gray>Toggled explosions."));
        }
        if (item.getType() == Material.WATER_BUCKET)
        {
            plugin.toggles.set("fluidspread", !plugin.toggles.getBoolean("fluidspread"));
            resetFluidspreadItem(inv);
            event.getWhoClicked().sendMessage(PlexUtils.mmDeserialize("<gray>Toggled fluid spread."));
        }
        if (item.getType() == Material.FEATHER)
        {
            plugin.toggles.set("drops", !plugin.toggles.getBoolean("drops"));
            resetDropsItem(inv);
            event.getWhoClicked().sendMessage(PlexUtils.mmDeserialize("<gray>Toggled drops."));
        }
        if (item.getType() == Material.REDSTONE)
        {
            plugin.toggles.set("redstone", !plugin.toggles.getBoolean("redstone"));
            resetRedstoneItem(inv);
            event.getWhoClicked().sendMessage(PlexUtils.mmDeserialize("<gray>Toggled redstone."));
        }
    }

    public int getCurrentInventoryIndex(Inventory inventory)
    {
        for (int i = 0; i <= inventories.size() - 1; i++)
        {
            if (inventories.get(i).hashCode() == inventory.hashCode())
            {
                return i;
            }
        }
        return 0;
    }

    private boolean isValidInventory(Inventory inventory)
    {
        return inventories.contains(inventory);
    }

    private void resetExplosionItem(Inventory inventory)
    {
        ItemStack explosions = new ItemStack(Material.TNT);
        ItemMeta explosionsItemMeta = explosions.getItemMeta();
        explosionsItemMeta.displayName(PlexUtils.mmDeserialize("<light_purple>Toggle explosions"));
        explosionsItemMeta.lore(List.of(PlexUtils.mmDeserialize("<yellow>Explosions are " + (plugin.toggles.getBoolean("explosions") ? "<red>enabled" : "<green>disabled"))));
        explosions.setItemMeta(explosionsItemMeta);
        inventory.setItem(0, explosions);
    }

    private void resetFluidspreadItem(Inventory inventory)
    {
        ItemStack water = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterItemMeta = water.getItemMeta();
        waterItemMeta.displayName(PlexUtils.mmDeserialize("<light_purple>Toggle fluid spread"));
        waterItemMeta.lore(List.of(PlexUtils.mmDeserialize("<yellow>Fluid spread is " + (plugin.toggles.getBoolean("fluidspread") ? "<green>enabled" : "<red>disabled"))));
        water.setItemMeta(waterItemMeta);
        inventory.setItem(1, water);
    }

    private void resetDropsItem(Inventory inventory)
    {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherItemMeta = feather.getItemMeta();
        featherItemMeta.displayName(PlexUtils.mmDeserialize("<light_purple>Toggle drops"));
        featherItemMeta.lore(List.of(PlexUtils.mmDeserialize("<yellow>Drops are " + (plugin.toggles.getBoolean("drops") ? "<green>enabled" : "<red>disabled"))));
        feather.setItemMeta(featherItemMeta);
        inventory.setItem(2, feather);
    }

    private void resetRedstoneItem(Inventory inventory)
    {
        ItemStack redstone = new ItemStack(Material.REDSTONE);
        ItemMeta redstoneItemMeta = redstone.getItemMeta();
        redstoneItemMeta.displayName(PlexUtils.mmDeserialize("<light_purple>Redstone"));
        redstoneItemMeta.lore(List.of(PlexUtils.mmDeserialize("<yellow>Redstone is " + (plugin.toggles.getBoolean("redstone") ? "<green>enabled" : "<red>disabled"))));
        redstone.setItemMeta(redstoneItemMeta);
        inventory.setItem(3, redstone);
    }
}
