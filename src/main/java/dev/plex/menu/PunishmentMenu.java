package dev.plex.menu;

import com.google.common.collect.Lists;
import dev.plex.cache.PlayerCache;
import dev.plex.player.PunishedPlayer;
import dev.plex.util.menu.AbstractMenu;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class PunishmentMenu extends AbstractMenu
{

    private List<Inventory> inventories = Lists.newArrayList();

    public PunishmentMenu() {
        super("§c§lPunishments");
        for (int i = 0; i <= Bukkit.getOnlinePlayers().size() / 53; i++)
        {
            Inventory inventory = Bukkit.createInventory(null, 54, "Punishments Page " + (i + 1));
            ItemStack nextPage = new ItemStack(Material.FEATHER);
            ItemMeta meta = nextPage.getItemMeta();
            meta.setDisplayName(ChatColor.LIGHT_PURPLE + "Next Page");
            nextPage.setItemMeta(meta);
            
            ItemStack previousPage = new ItemStack(Material.FEATHER);
            ItemMeta meta2 = previousPage.getItemMeta();
            meta2.setDisplayName(ChatColor.LIGHT_PURPLE + "Previous Page");
            previousPage.setItemMeta(meta2);
            
            inventory.setItem(50, nextPage);
            inventory.setItem(48, previousPage);
            inventories.add(inventory);
        }
    }

    public List<Inventory> getInventory() {
        return inventories;
    }

    public void openInv(Player player, int index) {
        int currentItemIndex = 0;
        int currentInvIndex = 0;
        for (Player players : Bukkit.getOnlinePlayers())
        {
            Inventory inv = inventories.get(currentInvIndex);
            if (currentInvIndex > inventories.size() - 1)
            {
                break;
            }

            if (currentItemIndex == inv.getSize() - 1) {
                currentInvIndex++;
                currentItemIndex = 0;
                inv = inventories.get(currentInvIndex);
            }


            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(players);
            meta.setDisplayName(ChatColor.YELLOW + players.getName());
            item.setItemMeta(meta);

            inv.setItem(currentItemIndex, item);

            currentItemIndex++;
        }
        player.openInventory(inventories.get(index));
    }

    @EventHandler
    public void onClick(InventoryClickEvent event)
    {
        if (event.getClickedInventory() == null) return;
        Inventory inv = event.getClickedInventory();
        if (!isValidInventory(inv)) return;
        if (event.getCurrentItem() == null) return;
        ItemStack item = event.getCurrentItem();
        event.setCancelled(true);
        if (item.getType() == Material.FEATHER)
        {
            if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.LIGHT_PURPLE + "Next Page"))
            {
                if (getCurrentInventoryIndex(inv) + 1 > inventories.size() - 1) return;
                if (inventories.get(getCurrentInventoryIndex(inv) + 1).getContents().length == 0) return;
                openInv((Player) event.getWhoClicked(), getCurrentInventoryIndex(inv) + 1);
            } else if (item.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.LIGHT_PURPLE + "Previous Page"))
            {
                if (getCurrentInventoryIndex(inv) - 1 < 0) return;
                if (getCurrentInventoryIndex(inv) - 1 > inventories.size() - 1) return;
                if (inventories.get(getCurrentInventoryIndex(inv) - 1).getContents().length == 0) return;
                openInv((Player) event.getWhoClicked(), getCurrentInventoryIndex(inv) - 1);
            }


        } else if (item.getType() == Material.PLAYER_HEAD)
        {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            OfflinePlayer player = meta.getOwningPlayer();
            assert player != null;
            PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId()) == null ? null : PlayerCache.getPunishedPlayer(player.getUniqueId());
            if (punishedPlayer == null)
            {
                event.getWhoClicked().sendMessage(ChatColor.RED + "This player does not exist. Try doing /punishments <player> instead.");
                event.getWhoClicked().closeInventory();
                return;
            }
            new PunishedPlayerMenu(punishedPlayer).openInv((Player) event.getWhoClicked(), 0);
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
}
