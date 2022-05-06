package dev.plex.api.chat;

import io.papermc.paper.event.player.AsyncChatEvent;

public interface IChatHandler
{
    void doChat(AsyncChatEvent event);
}
