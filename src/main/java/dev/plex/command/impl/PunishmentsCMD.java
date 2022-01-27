package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.PunishmentMenu;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
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
    public Component execute(CommandSender sender, String[] args)
    {
        Player player = sender.getPlayer();
        new PunishmentMenu().openInv(player, 0);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
