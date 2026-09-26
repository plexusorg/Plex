package dev.plex.world;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class WorldModificationPolicy
{
    private volatile Map<String, Restriction> restrictions = Map.of();

    public void reload(ConfigurationSection config)
    {
        Map<String, Restriction> next = new HashMap<>();
        ConfigurationSection worlds = config.getConfigurationSection("worlds");
        if (worlds != null)
        {
            for (String name : worlds.getKeys(false))
            {
                String permission = worlds.getString(name + ".modification.permission");
                String message = worlds.getString(name + ".modification.message");
                Component denial = permission == null || message == null || message.isBlank() ? null : MiniMessage.miniMessage().deserialize(message);
                next.putIfAbsent(name.toLowerCase(Locale.ROOT), new Restriction(permission, denial));
            }
        }
        restrictions = Map.copyOf(next);
    }

    public boolean canModify(Player player, String world)
    {
        Restriction restriction = restrictions.get(world.toLowerCase(Locale.ROOT));
        return restriction == null || restriction.allows(player);
    }

    public boolean denyModification(Player player, String world)
    {
        Restriction restriction = restrictions.get(world.toLowerCase(Locale.ROOT));
        if (restriction == null || restriction.allows(player))
        {
            return false;
        }
        if (player != null && restriction.message() != null)
        {
            player.sendMessage(restriction.message());
        }
        return true;
    }

    private record Restriction(String permission, Component message)
    {
        private boolean allows(Player player)
        {
            return permission == null || player != null && player.hasPermission(permission);
        }
    }
}
