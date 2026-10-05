package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.WrittenBookContent;
import io.papermc.paper.event.player.PlayerInsertLecternBookEvent;
import io.papermc.paper.text.Filtered;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.NBTComponent;
import net.kyori.adventure.text.SelectorComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.LecternInventory;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookListener extends ServerListenerBase
{
    public BookListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBookEdit(PlayerEditBookEvent event)
    {
        List<Component> pages = new ArrayList<>();

        for (Component page : event.getNewBookMeta().pages())
        {
            pages.add(SafeMiniMessage.mmDeserialize(PlexUtils.getTextFromComponent(page)));
        }


        event.setNewBookMeta((BookMeta) event.getNewBookMeta().pages(pages));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreativeItem(InventoryCreativeEvent event)
    {
        sanitizeBook(event.getCursor());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBookUse(PlayerInteractEvent event)
    {
        if (event.useItemInHand() == Event.Result.DENY)
        {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        {
            if (sanitizeBook(event.getItem()))
            {
                // Send the safe pages before Minecraft tells the client to open the held book.
                event.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLecternInsert(PlayerInsertLecternBookEvent event)
    {
        ItemStack book = event.getBook();
        if (sanitizeBook(book))
        {
            event.setBook(book);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLecternOpen(InventoryOpenEvent event)
    {
        if (event.getInventory() instanceof LecternInventory inventory)
        {
            sanitizeBook(inventory.getBook());
        }
    }

    private static boolean sanitizeBook(ItemStack item)
    {
        if (item == null)
        {
            return false;
        }
        WrittenBookContent content = item.getData(DataComponentTypes.WRITTEN_BOOK_CONTENT);
        if (content == null)
        {
            return false;
        }
        // Do not resolve selectors or interpreted NBT later: they can introduce new command click events.
        WrittenBookContent.Builder sanitized = WrittenBookContent.writtenBookContent(content.title(), content.author())
                .generation(content.generation()).resolved(true);
        for (Filtered<Component> page : content.pages())
        {
            sanitized.addFilteredPage(Filtered.of(sanitizePage(page.raw()),
                    page.filtered() == null ? null : sanitizePage(page.filtered())));
        }
        WrittenBookContent result = sanitized.build();
        if (content.equals(result))
        {
            return false;
        }
        item.setData(DataComponentTypes.WRITTEN_BOOK_CONTENT, result);
        return true;
    }

    private static Component sanitizePage(Component component)
    {
        ClickEvent click = component.clickEvent();
        if (click != null && (click.action() == ClickEvent.Action.RUN_COMMAND || click.action() == ClickEvent.Action.SUGGEST_COMMAND))
        {
            component = component.clickEvent(null);
        }
        List<Component> children = new ArrayList<>();
        for (Component child : component.children())
        {
            children.add(sanitizePage(child));
        }
        component = component.children(children);
        if (component.hoverEvent() != null)
        {
            component = component.hoverEvent(component.hoverEvent().withRenderedValue((text, context) -> sanitizePage(text), null));
        }
        return switch (component)
        {
            case TranslatableComponent translation ->
            {
                List<ComponentLike> arguments = new ArrayList<>();
                for (TranslationArgument argument : translation.arguments())
                {
                    arguments.add(argument.value() instanceof Component text ? sanitizePage(text) : argument);
                }
                yield translation.arguments(arguments);
            }
            case SelectorComponent selector when selector.separator() != null -> selector.separator(sanitizePage(selector.separator()));
            case NBTComponent<?> nbt when nbt.separator() != null -> nbt.separator(sanitizePage(nbt.separator()));
            default -> component;
        };
    }
}
