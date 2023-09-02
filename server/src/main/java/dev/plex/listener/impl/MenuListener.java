package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * @author Taah
 * @since 7:01 AM [02-09-2023]
 */
public class MenuListener extends PlexListener
{
    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent event)
    {
        if (event.getClickedInventory() == null)
        {
            return;
        }

        if (event.getCurrentItem() == null)
        {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player))
        {
            return;
        }

        AbstractMenu.INVENTORIES.entrySet().stream().filter(entry -> entry.getKey().equals(event.getView().title()))
                .findFirst()
                .ifPresent(entry ->
                {
                    if (entry.getValue() instanceof PageableMenu.Page page)
                    {
                        final ItemMeta meta = event.getCurrentItem().getItemMeta();
                        if (meta != null && meta.displayName() != null)
                        {
                            if (meta.displayName().equals(PageableMenu.PREVIOUS.getItemMeta().displayName()))
                            {
                                page.parent().currentPage(page.parent().currentPage() - 1);
                                page.parent().open(player, page.parent().currentPage());
                                event.setCancelled(true);
                                return;
                            }
                            else if (meta.displayName().equals(PageableMenu.NEXT.getItemMeta().displayName()))
                            {
                                page.parent().currentPage(page.parent().currentPage() + 1);
                                page.parent().open(player, page.parent().currentPage());
                                event.setCancelled(true);
                                return;
                            }
                            else if (meta.displayName().equals(PageableMenu.CLOSE.getItemMeta().displayName()))
                            {
                                player.closeInventory();
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                    event.setCancelled(entry.getValue().onClick(event.getView(), event.getClickedInventory(), player, event.getCurrentItem()));
                });
    }
}
