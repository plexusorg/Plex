package dev.plex.menu;

import com.google.common.collect.Maps;
import dev.plex.util.minimessage.SafeMiniMessage;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * @author Taah
 * @since 6:49 AM [02-09-2023]
 */

@Getter
@Accessors(fluent = true)
public abstract class AbstractMenu
{
    public static final Map<Component, AbstractMenu> INVENTORIES = Maps.newHashMap();
    private final Component name;
    private final Inventory inventory;

    public AbstractMenu(Component name, Rows rows)
    {
        this.name = name;
        this.inventory = Bukkit.createInventory(null, rows.slots, name);

        INVENTORIES.put(name, this);
    }

    public AbstractMenu(String name, Rows rows)
    {
        this(SafeMiniMessage.mmDeserializeWithoutEvents(name), rows);
    }

    public abstract boolean onClick(InventoryView view, Inventory inventory, Player player, ItemStack clicked);

    public void open(Player player)
    {
        player.openInventory(this.inventory);
    }

    public enum Rows
    {
        ONE(9), TWO(18), THREE(27), FOUR(36), FIVE(45), SIX(54);

        private final int slots;

        Rows(int slots)
        {
            this.slots = slots;
        }

        public int slots()
        {
            return this.slots;
        }
    }

}
