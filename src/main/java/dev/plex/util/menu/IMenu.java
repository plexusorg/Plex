package dev.plex.util.menu;


import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface IMenu
{

    Inventory getInventory();


    void openInv(Player player);

    @EventHandler
    void onClick(InventoryClickEvent event);

}
