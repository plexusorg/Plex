package dev.plex.listener.impl;

import dev.plex.Plex;

import dev.plex.api.event.PlayerPrefixEvent;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
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
        // Follow external nickname changes and time-limited prefixes on the entity owner.
        // The entity scheduler retires this task on disconnect; there is no retained task state.
        player.getScheduler().runAtFixedRate(plugin, task -> render(player), null, 20, 20);
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
