package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignEdit(SignChangeEvent event)
    {
        for (int i = 0; i < event.lines().size(); i++)
        {
            event.line(i, SafeMiniMessage.mmDeserialize(PlexUtils.getTextFromComponent(event.line(i))));
        }
    }
}
