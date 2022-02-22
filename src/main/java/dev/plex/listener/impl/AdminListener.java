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
import static dev.plex.util.PlexUtils.tl;

public class AdminListener extends PlexListener
{
    @EventHandler
    public void onAdminAdd(AdminAddEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        target.setRank(Rank.ADMIN.name());
        DataUtils.update(target);
        PlexUtils.broadcast(tl("newAdminAdded", userSender, target.getName()));
    }

    @EventHandler
    public void onAdminRemove(AdminRemoveEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        target.setRank("");
        DataUtils.update(target);
        PlexUtils.broadcast(tl("adminRemoved", userSender, target.getName()));
    }

    @EventHandler
    public void onAdminSetRank(AdminSetRankEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        Rank newRank = event.getRank();
        target.setRank(newRank.name().toLowerCase());
        DataUtils.update(target);
        PlexUtils.broadcast(tl("adminSetRank", userSender, target.getName(), newRank.getReadable()));
    }
}
