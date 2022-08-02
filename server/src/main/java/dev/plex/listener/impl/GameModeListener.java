package dev.plex.listener.impl;

import dev.plex.event.GameModeUpdateEvent;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

public class GameModeListener extends PlexListener
{
    @EventHandler
    public void onGameModeUpdate(GameModeUpdateEvent event)
    {
        CommandSender userSender = event.getSender();
        Player target = event.getPlayer();
        if (userSender.getName().equals(target.getName()))
        {
            target.setGameMode(event.getGameMode());
            userSender.sendMessage(PlexUtils.messageComponent("gameModeSetTo", event.getGameMode().toString().toLowerCase()));
        }
        else
        {
            target.sendMessage(PlexUtils.messageComponent("playerSetOtherGameMode", userSender.getName(), event.getGameMode().toString().toLowerCase()));
            target.setGameMode(event.getGameMode());
            userSender.sendMessage(PlexUtils.messageComponent("setOtherPlayerGameModeTo", target.getName(), event.getGameMode().toString().toLowerCase()));
        }
    }
}
