package dev.plex.event;

import lombok.Data;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Data
public class GameModeUpdateEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final CommandSender sender;

    private final Player player;

    private final GameMode gameMode;

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}
