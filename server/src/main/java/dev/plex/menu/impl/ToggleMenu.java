package dev.plex.menu.impl;

import dev.plex.Plex;
import dev.plex.menu.AbstractMenu;
import dev.plex.util.PlexUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ToggleMenu extends AbstractMenu
{
    private final Plex plugin;

    public ToggleMenu()
    {
        super("<green><bold>Toggles", Rows.ONE);
        this.plugin = Plex.get();
        resetExplosionItem(this.inventory());
        resetFluidspreadItem(this.inventory());
        resetDropsItem(this.inventory());
        resetRedstoneItem(this.inventory());
    }

    private void resetExplosionItem(Inventory inventory)
    {
        ItemStack explosions = new ItemStack(Material.TNT);
        ItemMeta explosionsItemMeta = explosions.getItemMeta();
        explosionsItemMeta.displayName(PlexUtils.mmDeserialize("<!italic><light_purple>Toggle explosions"));
        explosionsItemMeta.lore(List.of(PlexUtils.mmDeserialize("<!italic><yellow>Explosions are " + (plugin.toggles.getBoolean("explosions") ? "<red>enabled" : "<green>disabled"))));
        explosions.setItemMeta(explosionsItemMeta);
        inventory.setItem(0, explosions);
    }

    private void resetFluidspreadItem(Inventory inventory)
    {
        ItemStack water = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterItemMeta = water.getItemMeta();
        waterItemMeta.displayName(PlexUtils.mmDeserialize("<!italic><light_purple>Toggle fluid spread"));
        waterItemMeta.lore(List.of(PlexUtils.mmDeserialize("<!italic><yellow>Fluid spread is " + (plugin.toggles.getBoolean("fluidspread") ? "<green>enabled" : "<red>disabled"))));
        water.setItemMeta(waterItemMeta);
        inventory.setItem(1, water);
    }

    private void resetDropsItem(Inventory inventory)
    {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherItemMeta = feather.getItemMeta();
        featherItemMeta.displayName(PlexUtils.mmDeserialize("<!italic><light_purple>Toggle drops"));
        featherItemMeta.lore(List.of(PlexUtils.mmDeserialize("<!italic><yellow>Drops are " + (plugin.toggles.getBoolean("drops") ? "<green>enabled" : "<red>disabled"))));
        feather.setItemMeta(featherItemMeta);
        inventory.setItem(2, feather);
    }

    private void resetRedstoneItem(Inventory inventory)
    {
        ItemStack redstone = new ItemStack(Material.REDSTONE);
        ItemMeta redstoneItemMeta = redstone.getItemMeta();
        redstoneItemMeta.displayName(PlexUtils.mmDeserialize("<!italic><light_purple>Redstone"));
        redstoneItemMeta.lore(List.of(PlexUtils.mmDeserialize("<!italic><yellow>Redstone is " + (plugin.toggles.getBoolean("redstone") ? "<green>enabled" : "<red>disabled"))));
        redstone.setItemMeta(redstoneItemMeta);
        inventory.setItem(3, redstone);
    }

    @Override
    public boolean onClick(InventoryView view, Inventory inventory, Player player, ItemStack clicked)
    {
        if (clicked.getType() == Material.TNT)
        {
            plugin.toggles.set("explosions", !plugin.toggles.getBoolean("explosions"));
            resetExplosionItem(inventory);
            player.sendMessage(PlexUtils.mmDeserialize("<gray>Toggled explosions."));
        }
        if (clicked.getType() == Material.WATER_BUCKET)
        {
            plugin.toggles.set("fluidspread", !plugin.toggles.getBoolean("fluidspread"));
            resetFluidspreadItem(inventory);
            player.sendMessage(PlexUtils.mmDeserialize("<gray>Toggled fluid spread."));
        }
        if (clicked.getType() == Material.FEATHER)
        {
            plugin.toggles.set("drops", !plugin.toggles.getBoolean("drops"));
            resetDropsItem(inventory);
            player.sendMessage(PlexUtils.mmDeserialize("<gray>Toggled drops."));
        }
        if (clicked.getType() == Material.REDSTONE)
        {
            plugin.toggles.set("redstone", !plugin.toggles.getBoolean("redstone"));
            resetRedstoneItem(inventory);
            player.sendMessage(PlexUtils.mmDeserialize("<gray>Toggled redstone."));
        }
        return true;
    }
}
