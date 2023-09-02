package dev.plex.menu.pagination;

import com.google.common.collect.Maps;
import dev.plex.menu.AbstractMenu;
import dev.plex.util.PlexUtils;
import dev.plex.util.function.ConditionalQuadConsumer;
import dev.plex.util.item.ItemBuilder;
import dev.plex.util.minimessage.SafeMiniMessage;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * @author Taah
 * @since 8:04 AM [02-09-2023]
 */

@Getter
@Setter
@Accessors(fluent = true)
public abstract class PageableMenu<T>
{
    public static final ItemStack NEXT = new ItemBuilder(Material.FEATHER).displayName("<light_purple>Next Page").build();
    public static final ItemStack PREVIOUS = new ItemBuilder(Material.FEATHER).displayName("<light_purple>Previous Page").build();
    public static final ItemStack CLOSE = new ItemBuilder(Material.BARRIER).displayName("<red>Close").build();
    protected final Map<Integer, Page> pages = Maps.newHashMap();
    private final Component name;
    private final AbstractMenu.Rows rows;
    private final Inventory inventory;
    private int currentPage;
    private boolean initialized;

    @Setter(AccessLevel.NONE)
    private ConditionalQuadConsumer<InventoryView, Inventory, Player, ItemStack> onClick;

    public PageableMenu(Component name, AbstractMenu.Rows rows)
    {
        this.name = name;
        this.rows = rows;
        this.inventory = Bukkit.createInventory(null, rows.slots(), name);

        if (rows.slots() < AbstractMenu.Rows.TWO.slots())
        {
            throw new RuntimeException("A pageable menu must have at least two rows to compensate for the navigation!");
        }
    }

    protected void init()
    {
        this.initialized = true;
        // Preload all needed pages, a better solution is probably just to dynamically create the pages as they go but no!
        if (list().size() < (rows.slots() - 9))
        {
            final Page page = new Page(name.append(Component.space()).append(PlexUtils.mmDeserialize("(1)")), rows);
            page.parent = this;
            page.inventory().setItem(rows.slots() - 5, CLOSE);
            this.pages.put(0, page);
        }
        else
        {
            for (int i = 0; i < Math.ceil(list().size() / (double) (rows.slots() - 9)); i++)
            {
                final Page page = new Page(name.append(Component.space()).append(PlexUtils.mmDeserialize("(" + (i + 1) + ")")), rows);
                page.parent = this;
                if (i > 0) // If not first page set previous page button
                {
                    page.inventory().setItem(rows.slots() - 6, PREVIOUS);
                }
                page.inventory().setItem(rows.slots() - 5, CLOSE);
                if (i < (list().size() / (rows.slots() - 9)) - 1) // If not last page set next page button
                {
                    page.inventory().setItem(rows.slots() - 4, NEXT);
                }
                this.pages.put(i, page);
            }
        }

        int currentSlotIndex = 0;
        int currentPageIndex = 0;
        int currentIndex = 0;
        while (currentSlotIndex < rows.slots() - 9 && currentIndex < list().size())
        {
            final Page page = this.pages.get(currentPageIndex);
            if (page == null)
            {
                return;
            }
            page.inventory().setItem(currentSlotIndex, toItem(list().get(currentIndex)));
            currentIndex++;
            currentSlotIndex++;
            if (currentSlotIndex == rows.slots() - 9)
            {
                currentSlotIndex = 0;
                currentPageIndex++;
            }
        }
        if (onClick != null) // To make sure this wasn't declared already
        {
            this.pages.forEach((integer, page) -> page.onClick(this.onClick));
        }
    }

    public PageableMenu(String name, AbstractMenu.Rows rows)
    {
        this(SafeMiniMessage.mmDeserializeWithoutEvents(name), rows);
    }

    protected abstract ItemStack toItem(T object);

    protected abstract List<T> list();

    public void open(Player player)
    {
        open(player, 0);
    }

    public void open(Player player, int pageNum)
    {
        if (!this.initialized)
        {
            player.sendMessage(Component.text("Looks like this inventory was not initialized! Please contact a developer to report this.").color(NamedTextColor.RED));
            return;
        }
        final Page page = this.pages.get(pageNum);
        if (page == null)
        {
            player.sendMessage(Component.text("Could not find a page to open").color(NamedTextColor.RED));
            return;
        }
        player.openInventory(page.inventory());
    }

    public void onClick(ConditionalQuadConsumer<InventoryView, Inventory, Player, ItemStack> onClick)
    {
        this.onClick = onClick;
        this.pages.forEach((integer, page) -> page.onClick(this.onClick));
    }


    @Getter
    @Setter
    @Accessors(fluent = true)
    public static class Page extends AbstractMenu
    {
        private ConditionalQuadConsumer<InventoryView, Inventory, Player, ItemStack> onClick;
        private PageableMenu<?> parent;

        private Page(Component name, Rows rows)
        {
            super(name, rows);
        }

        private Page(String name, AbstractMenu.Rows rows)
        {
            this(SafeMiniMessage.mmDeserializeWithoutEvents(name), rows);
        }

        @Override
        public boolean onClick(InventoryView view, Inventory inventory, Player player, ItemStack clicked)
        {
            return onClick.accept(view, inventory, player, clicked);
        }
    }
}
