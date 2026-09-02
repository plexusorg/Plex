package dev.plex.listener.impl;

import dev.plex.Plex;

import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;

import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class PlayerListener extends ServerListenerBase
{
    public PlayerListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerPreLogin(AsyncPlayerPreLoginEvent event)
    {
        try
        {
            plugin.getPlayerService().prepareSession(event.getUniqueId(), event.getName(),
                    event.getAddress().getHostAddress().trim()).join();
        }
        catch (CompletionException failure)
        {
            PlexLog.error("Unable to prepare player session for {0}: {1}", event.getUniqueId(), failure.getMessage());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Unable to load your player data. Please try again shortly."));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        PlexPlayer plexPlayer = plugin.getPlayerService().attachPreparedSession(player.getUniqueId());
        if (plexPlayer == null)
        {
            PlexLog.error("No prepared Plex session exists for {0}; disconnecting safely.", player.getName());
            player.kick(Component.text("Your player data was not ready. Please reconnect."));
            return;
        }
        plugin.getPunishmentManager().restoreTimedState(plexPlayer);

        if (plexPlayer.isLockedUp())
        {
            player.openInventory(player.getInventory());
        }

        String loginMessage = PlayerMeta.getLoginMessage(plugin.config, plexPlayer);
        if (!loginMessage.isEmpty() && !PlayerMeta.isVanished(player))
        {
            PlexUtils.broadcast(PlexUtils.stringToComponent(loginMessage));
        }

        plugin.getNoteRepository().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
        {
            if (ex != null)
            {
                PlexLog.warn("Unable to load notes for {0}: {1}", plexPlayer.getUuid(), ex.getMessage());
                return;
            }
            if (!notes.isEmpty())
            {
                if (plugin.getPlayerService().getCachedPlayer(plexPlayer.getUuid()) == plexPlayer)
                {
                    PlexUtils.broadcastToAdmins(PlexUtils.messageComponent(notes.size() == 1 ? "playerNoteAlert" : "playerNoteAlertPlural", plexPlayer.getName(), notes.size()), "plex.notes.notify");
                }
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSave(PlayerQuitEvent event)
    {
        plugin.getPlayerService().detachAndSave(event.getPlayer().getUniqueId()).exceptionally(failure ->
        {
            PlexLog.error("Unable to save player {0}: {1}", event.getPlayer().getUniqueId(), failure.getMessage());
            return null;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInventoryClose(InventoryCloseEvent event)
    {
        PlexPlayer player = plugin.getPlayerService().getCachedPlayer(event.getPlayer().getUniqueId());
        if (player != null && player.isLockedUp())
        {
            event.getPlayer().getScheduler().runDelayed(plugin, scheduledTask -> event.getPlayer().openInventory(event.getInventory()), null, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        PlexPlayer player = plugin.getPlayerService().getCachedPlayer(event.getWhoClicked().getUniqueId());
        if (player != null && player.isLockedUp())
        {
            event.setCancelled(true);
        }
    }
}
