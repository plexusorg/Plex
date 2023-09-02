package dev.plex.menu.impl;

import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import dev.plex.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * @author Taah
 * @since 8:44 AM [02-09-2023]
 */
public class MaterialMenu extends PageableMenu<Material>
{
    public MaterialMenu()
    {
        super("<dark_aqua><bold>Materials", AbstractMenu.Rows.SIX);
        onClick((inventoryView, inventory, player, itemStack) ->
        {
            player.sendMessage(itemStack.displayName());
            return true;
        });
        this.init();
    }

    @Override
    protected ItemStack toItem(Material object)
    {
        return new ItemBuilder(object).displayName("<blue>" + object.name()).build();
    }

    @Override
    protected List<Material> list()
    {
        return Arrays.stream(Material.values()).filter(material -> !material.isAir()).toList();
    }
}
