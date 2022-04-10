package dev.plex.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
@Data
public class GameModeUpdateEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final CommandSender sender;

    private final Player player;

    private final GameMode gameMode;

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
