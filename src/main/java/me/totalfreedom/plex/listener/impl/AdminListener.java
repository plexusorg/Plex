package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.event.AdminAddEvent;
import me.totalfreedom.plex.event.AdminRemoveEvent;
import me.totalfreedom.plex.event.AdminSetRankEvent;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;

public class AdminListener extends PlexListener
{

    @EventHandler
    public void onAdminAdd(AdminAddEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();

        Bukkit.broadcastMessage(String.format(ChatColor.RED + "%s - Adding %s to the admin list!", userSender, target.getName()));
    }

    @EventHandler
    public void onAdminRemove(AdminRemoveEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();

        Bukkit.broadcastMessage(String.format(ChatColor.RED + "%s - Removing %s from the admin list!", userSender, target.getName()));
    }

    @EventHandler
    public void onAdminSetrank(AdminSetRankEvent event)
    {
        String userSender = event.getSender().getName();
        PlexPlayer target = event.getPlexPlayer();
        Rank newRank = event.getRank();

        Bukkit.broadcastMessage(String.format(ChatColor.RED + "%s - Setting %s's rank to %s!", userSender, target.getName(), newRank.name().toUpperCase()));
    }

}
