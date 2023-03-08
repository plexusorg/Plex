package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.event.AdminAddEvent;
import dev.plex.event.AdminRemoveEvent;
import dev.plex.event.AdminSetRankEvent;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.event.EventHandler;

import static dev.plex.util.PlexUtils.messageComponent;

public class AdminListener extends PlexListener
{
    @EventHandler
    public void onAdminAdd(AdminAddEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        if (target.getRankFromString().isAtLeast(Rank.ADMIN))
        {
            PlexUtils.broadcast(messageComponent("adminReadded", userSender, target.getName(), target.getRankFromString().getReadable()));
        }
        else
        {
            target.setRank(Rank.ADMIN.name());
            PlexUtils.broadcast(messageComponent("newAdminAdded", userSender, target.getName()));
        }
        target.setAdminActive(true);
        DataUtils.update(target);
    }

    @EventHandler
    public void onAdminRemove(AdminRemoveEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        target.setAdminActive(false);
        DataUtils.update(target);
        PlexUtils.broadcast(messageComponent("adminRemoved", userSender, target.getName()));
    }

    @EventHandler
    public void onAdminSetRank(AdminSetRankEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        Rank newRank = event.getRank();
        target.setRank(newRank.name().toLowerCase());
        DataUtils.update(target);
        PlexUtils.broadcast(messageComponent("adminSetRank", userSender, target.getName(), newRank.getReadable()));
    }
}
