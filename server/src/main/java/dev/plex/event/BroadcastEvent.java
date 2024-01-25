package dev.plex.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
@Data
public class BroadcastEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final Component message;

    private final String string;

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }
}
