package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.menu.PunishmentMenu;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.entity.Player;

import java.util.List;

@CommandParameters(usage = "/<command> [player]", description = "Opens the Punishments GUI", aliases = "punishlist,punishes")
@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
public class PunishmentsCMD extends PlexCommand
{

    public PunishmentsCMD() {
        super("punishments");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        Player player = sender.getPlayer();
        new PunishmentMenu().openInv(player, 0);
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
