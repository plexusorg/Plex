package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.CommandArgumentException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.player.PunishedPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.entity.Player;

import java.util.List;

import static me.totalfreedom.plex.util.PlexUtils.tl;

@CommandParameters(description = "Freeze/unfreeze a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN)
public class FreezeCMD extends PlexCommand
{
    public FreezeCMD()
    {
        super("freeze");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length != 1)
            throw new CommandArgumentException();
        Player player = getNonNullPlayer(args[0]);
        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        punishedPlayer.setFrozen(!punishedPlayer.isFrozen());
        PlexUtils.broadcast(punishedPlayer.isFrozen() ? tl("frozePlayer", sender.getName(), player.getName()) :
                tl("unfrozePlayer", sender.getName(), player.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : null;
    }
}