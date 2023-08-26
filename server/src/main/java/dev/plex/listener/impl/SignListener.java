package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.SignChangeEvent;

public class SignListener extends PlexListener
{
    private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSignEdit(SignChangeEvent event)
    {
        for (int i = 0; i < event.lines().size(); i++)
        {
            event.line(i, LEGACY_COMPONENT_SERIALIZER.deserialize(PlexUtils.getTextFromComponent(event.line(i))));
        }
    }
}
