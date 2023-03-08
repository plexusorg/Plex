package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.List;

public class BookListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBookEdit(PlayerEditBookEvent event)
    {
        List<Component> pages = new ArrayList<>();

        for (Component page : event.getNewBookMeta().pages())
        {
            pages.add(SafeMiniMessage.mmDeserializeWithoutEvents(PlexUtils.getTextFromComponent(page)));
        }


        event.setNewBookMeta((BookMeta) event.getNewBookMeta().pages(pages));
    }
}
