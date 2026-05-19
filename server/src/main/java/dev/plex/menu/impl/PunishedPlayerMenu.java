package dev.plex.menu.impl;

import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import dev.plex.player.PlayerService;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.item.ItemBuilder;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class PunishedPlayerMenu extends PageableMenu<Punishment>
{
    private final PlexPlayer punishedPlayer;
    private final PlayerService playerService;

    public PunishedPlayerMenu(PlexPlayer player, PlayerService playerService)
    {
        super(PlexUtils.messageComponent("punishedPlayerMenuTitle", player.getName()), AbstractMenu.Rows.SIX);
        this.punishedPlayer = player;
        this.playerService = playerService;
        onClick((inventoryView, itemStacks, player1, itemStack) -> true);
        this.init();
    }

    @Override
    protected ItemStack toItem(Punishment object)
    {
        return new ItemBuilder(Material.PAPER)
                .displayName(PlexUtils.messageComponent("punishmentItemTitle", object.getType().name()))
                .lore(PlexUtils.messageComponent("punishmentItemPunisher", object.getPunisher() == null ? "CONSOLE" : playerService.getNameByUUID(object.getPunisher())),
                        PlexUtils.messageComponent("punishmentItemIssued", TimeUtils.useTimezone(object.getIssueDate())),
                        PlexUtils.messageComponent("punishmentItemExpires", TimeUtils.useTimezone(object.getEndDate())),
                        PlexUtils.messageComponent("punishmentItemReason", object.getReason()))
                .build();
    }

    @Override
    protected List<Punishment> list()
    {
        return this.punishedPlayer.getPunishments();
    }
}
