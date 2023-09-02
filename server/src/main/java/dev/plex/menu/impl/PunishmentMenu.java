package dev.plex.menu.impl;

import dev.plex.cache.DataUtils;
import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.item.ItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * @author Taah
 * @since 9:27 AM [02-09-2023]
 */
public class PunishmentMenu extends PageableMenu<Player>
{
    public PunishmentMenu()
    {
        super("<aqua><bold>Punishments", AbstractMenu.Rows.SIX);
        PlexLog.debug("list: {0}", list().size());
        onClick((inventoryView, itemStacks, player, itemStack) ->
        {
            if (itemStack.getType() != Material.PLAYER_HEAD) return true;
            final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
            if (meta.getOwningPlayer() == null)
            {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer("markbyron"));
            }
            PlexPlayer punishedPlayer = DataUtils.getPlayer(meta.getOwningPlayer().getUniqueId());
            if (punishedPlayer == null)
            {
                player.sendMessage(Component.text("This player does not exist. Try doing /punishments <player> instead.").color(NamedTextColor.RED));
                player.closeInventory();
                return true;
            }
            new PunishedPlayerMenu(punishedPlayer).open(player);
            return true;
        });

        this.init();
    }

    @Override
    protected ItemStack toItem(Player object)
    {
        return new ItemBuilder(Material.PLAYER_HEAD).owner(object).displayName("<!italic><yellow>" + object.getName()).build();
    }

    @Override
    protected List<Player> list()
    {
        return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getPlayer).toList();
    }
}
