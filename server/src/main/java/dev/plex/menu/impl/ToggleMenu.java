package dev.plex.menu.impl;

import dev.plex.Plex;
import dev.plex.menu.AbstractMenu;
import dev.plex.util.PlexUtils;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToggleMenu extends AbstractMenu
{
    private final Plex plugin;

    public ToggleMenu(Plex plugin)
    {
        super(PlexUtils.messageComponent("toggleMenuTitle"), Rows.ONE);
        this.plugin = plugin;
        resetExplosionItem(this.inventory());
        resetFluidspreadItem(this.inventory());
        resetDropsItem(this.inventory());
        resetRedstoneItem(this.inventory());
        resetPVPItem(this.inventory());
        resetChatItem(this.inventory());
    }

    private void resetExplosionItem(Inventory inventory)
    {
        ItemStack explosions = new ItemStack(Material.TNT);
        ItemMeta explosionsItemMeta = explosions.getItemMeta();
        explosionsItemMeta.displayName(PlexUtils.messageComponent("toggleMenuExplosionsName"));
        explosionsItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuExplosionsLore", status("explosions", true))));
        explosions.setItemMeta(explosionsItemMeta);
        inventory.setItem(0, explosions);
    }

    private void resetFluidspreadItem(Inventory inventory)
    {
        ItemStack water = new ItemStack(Material.WATER_BUCKET);
        ItemMeta waterItemMeta = water.getItemMeta();
        waterItemMeta.displayName(PlexUtils.messageComponent("toggleMenuFluidSpreadName"));
        waterItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuFluidSpreadLore", status("fluidspread", false))));
        water.setItemMeta(waterItemMeta);
        inventory.setItem(1, water);
    }

    private void resetDropsItem(Inventory inventory)
    {
        ItemStack feather = new ItemStack(Material.FEATHER);
        ItemMeta featherItemMeta = feather.getItemMeta();
        featherItemMeta.displayName(PlexUtils.messageComponent("toggleMenuDropsName"));
        featherItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuDropsLore", status("drops", false))));
        feather.setItemMeta(featherItemMeta);
        inventory.setItem(2, feather);
    }

    private void resetRedstoneItem(Inventory inventory)
    {
        ItemStack redstone = new ItemStack(Material.REDSTONE);
        ItemMeta redstoneItemMeta = redstone.getItemMeta();
        redstoneItemMeta.displayName(PlexUtils.messageComponent("toggleMenuRedstoneName"));
        redstoneItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuRedstoneLore", status("redstone", false))));
        redstone.setItemMeta(redstoneItemMeta);
        inventory.setItem(3, redstone);
    }

    private void resetPVPItem(Inventory inventory)
    {
        ItemStack pvp = new ItemStack(Material.IRON_SWORD);
        ItemMeta pvpItemMeta = pvp.getItemMeta();
        pvpItemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        pvpItemMeta.displayName(PlexUtils.messageComponent("toggleMenuPvpName"));
        pvpItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuPvpLore", status("pvp", false))));
        pvp.setItemMeta(pvpItemMeta);
        inventory.setItem(4, pvp);
    }

    private void resetChatItem(Inventory inventory)
    {
        ItemStack chat = new ItemStack(Material.OAK_SIGN);
        ItemMeta chatItemMeta = chat.getItemMeta();
        chatItemMeta.displayName(PlexUtils.messageComponent("toggleMenuChatName"));
        chatItemMeta.lore(List.of(PlexUtils.messageComponent("toggleMenuChatLore", PlexUtils.messageString(plugin.toggles.getBoolean("chat") ? "stateOn" : "stateOff"))));
        chat.setItemMeta(chatItemMeta);
        inventory.setItem(5, chat);
    }

    @Override
    public boolean onClick(InventoryView view, Inventory inventory, Player player, ItemStack clicked)
    {
        if (!player.hasPermission("plex.toggle"))
        {
            return false;
        }
        if (clicked.getType() == Material.TNT)
        {
            plugin.toggles.set("explosions", !plugin.toggles.getBoolean("explosions"));
            resetExplosionItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("toggleExplosionsLower")));
        }
        if (clicked.getType() == Material.WATER_BUCKET)
        {
            plugin.toggles.set("fluidspread", !plugin.toggles.getBoolean("fluidspread"));
            resetFluidspreadItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("toggleFluidSpreadLower")));
        }
        if (clicked.getType() == Material.FEATHER)
        {
            plugin.toggles.set("drops", !plugin.toggles.getBoolean("drops"));
            resetDropsItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("toggleDropsLower")));
        }
        if (clicked.getType() == Material.REDSTONE)
        {
            plugin.toggles.set("redstone", !plugin.toggles.getBoolean("redstone"));
            resetRedstoneItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("toggleRedstoneLower")));
        }
        if (clicked.getType() == Material.IRON_SWORD)
        {
            plugin.toggles.set("pvp", !plugin.toggles.getBoolean("pvp"));
            resetPVPItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("togglePvpLower")));
        }
        if (clicked.getType() == Material.OAK_SIGN)
        {
            plugin.toggles.set("chat", !plugin.toggles.getBoolean("chat"));
            PlexUtils.broadcast(PlexUtils.messageComponent("chatToggled", player.getName(), PlexUtils.messageString(plugin.toggles.getBoolean("chat") ? "stateOn" : "stateOff")));
            resetChatItem(inventory);
            player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString("toggleChatLower")));
        }
        return true;
    }

    private String status(String toggle, boolean enabledIsUnsafe)
    {
        if (enabledIsUnsafe)
        {
            return PlexUtils.messageString(plugin.toggles.getBoolean(toggle) ? "stateEnabledUnsafe" : "stateDisabledSafe");
        }
        return PlexUtils.messageString(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled");
    }
}
