package dev.plex.menu.impl;

import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import dev.plex.player.PlayerService;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.item.ItemBuilder;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class PunishmentMenu extends PageableMenu<Player>
{
    private final PlayerService playerService;

    public PunishmentMenu(PlayerService playerService)
    {
        super(PlexUtils.messageComponent("punishmentMenuTitle"), AbstractMenu.Rows.SIX);
        this.playerService = playerService;
        PlexLog.debug("list: {0}", list().size());
        onClick((inventoryView, itemStacks, player, itemStack) ->
        {
            if (itemStack.getType() != Material.PLAYER_HEAD)
            {
                return true;
            }
            final SkullMeta meta = (SkullMeta) itemStack.getItemMeta();
            if (meta.getOwningPlayer() == null)
            {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer("markbyron"));
            }
            PlexPlayer punishedPlayer = playerService.getPlayer(meta.getOwningPlayer().getUniqueId());
            if (punishedPlayer == null)
            {
                player.sendMessage(PlexUtils.messageComponent("punishmentPlayerNotFound"));
                player.closeInventory();
                return true;
            }
            new PunishedPlayerMenu(punishedPlayer, playerService).open(player);
            return true;
        });

        this.init();
    }

    @Override
    protected ItemStack toItem(Player object)
    {
        return new ItemBuilder(Material.PLAYER_HEAD).owner(object).displayName(PlexUtils.messageComponent("punishmentPlayerItem", object.getName())).build();
    }

    @Override
    protected List<Player> list()
    {
        return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getPlayer).toList();
    }
}
