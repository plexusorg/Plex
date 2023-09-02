package dev.plex.menu.impl;

import dev.plex.Plex;
import dev.plex.menu.AbstractMenu;
import dev.plex.menu.pagination.PageableMenu;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.util.TimeUtils;
import dev.plex.util.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class PunishedPlayerMenu extends PageableMenu<Punishment>
{
    private final PlexPlayer punishedPlayer;
    public PunishedPlayerMenu(PlexPlayer player)
    {
        super("<red><bold>Punishments - " + player.getName(), AbstractMenu.Rows.SIX);
        this.punishedPlayer = player;
        onClick((inventoryView, itemStacks, player1, itemStack) -> true);
        this.init();
    }

    @Override
    protected ItemStack toItem(Punishment object)
    {
        return new ItemBuilder(Material.PAPER).displayName("<!italic><red>" + object.getType().name()).lore("<!italic><red>By: <gold>" + (object.getPunisher() == null ? "CONSOLE" : Plex.get().getSqlPlayerData().getNameByUUID(object.getPunished())), "<!italic><red>Expire(d/s): <gold>" + TimeUtils.useTimezone(object.getEndDate()), "<!italic><red>Reason: <gold>" + object.getReason()).build();
    }

    @Override
    protected List<Punishment> list()
    {
        return this.punishedPlayer.getPunishments();
    }
}
