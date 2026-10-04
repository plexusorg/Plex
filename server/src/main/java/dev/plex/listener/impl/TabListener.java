package dev.plex.listener.impl;

import dev.plex.Plex;

import dev.plex.api.event.PlayerPrefixEvent;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabListener extends ServerListenerBase
{
    public TabListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        render(player);
        Set<UUID> unlisted = new HashSet<>();
        // Follow external nickname changes and time-limited prefixes on the entity owner.
        // The viewer task owns Plex's unlisted entries and retires on disconnect.
        // The first run waits one tick so Paper has sent the viewer's initial player list.
        player.getScheduler().runAtFixedRate(plugin, task ->
        {
            render(player);
            reconcileListings(player, unlisted);
        }, null, 1, 20);
    }

    private void reconcileListings(Player viewer, Set<UUID> unlisted)
    {
        Set<UUID> banned = plugin.getPunishmentManager().finiteBanRestrictedOnlinePlayers();
        Set<UUID> targets = new HashSet<>(unlisted);
        targets.addAll(banned);
        boolean admin = viewer.hasPermission("plex.ban");
        for (UUID uuid : targets)
        {
            if (uuid.equals(viewer.getUniqueId())) continue;
            Player target = Bukkit.getPlayer(uuid);
            if (target == null)
            {
                unlisted.remove(uuid);
                continue;
            }
            if (admin || !banned.contains(uuid))
            {
                // Paper cannot list a player hidden by a vanish plugin.
                if (!viewer.canSee(target)) continue;
                if (!viewer.isListed(target)) viewer.listPlayer(target);
                unlisted.remove(uuid);
            }
            else if (viewer.isListed(target))
            {
                viewer.unlistPlayer(target);
                unlisted.add(uuid);
            }
        }
    }

    private void render(Player player)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(player.getUniqueId());
        Component name = player.displayName();
        if (name.equals(Component.text(player.getName())))
        {
            name = PlexUtils.mmDeserialize(PlayerMeta.getColor(plugin.config, plexPlayer) + player.getName());
        }
        String customTag = plexPlayer.getPrefix();
        Component tag = customTag == null || customTag.isEmpty()
                ? Component.empty() : SafeMiniMessage.mmDeserialize(customTag);
        PlayerPrefixEvent renderEvent = new PlayerPrefixEvent(player, PlayerPrefixEvent.Target.TAB, name, tag, false);
        plugin.getServer().getPluginManager().callEvent(renderEvent);
        Component entry = Component.empty();
        if (plugin.getPunishmentManager().isFiniteBanRestricted(player.getUniqueId()))
        {
            entry = entry.append(PlexUtils.messageComponent("bannedTabMarker")).append(Component.space());
        }
        for (Component prefix : renderEvent.getPrefixes())
        {
            entry = entry.append(prefix).append(Component.space());
        }
        if (!tag.equals(Component.empty()) && !tag.equals(Component.space()))
        {
            entry = entry.append(tag).append(Component.space());
        }
        entry = entry.append(name);
        if (!entry.equals(player.playerListName()))
        {
            player.playerListName(entry);
        }
    }

}
