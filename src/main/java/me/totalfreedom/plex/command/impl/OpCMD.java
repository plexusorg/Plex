package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import java.util.List;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.CommandArgumentException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.entity.Player;
import static me.totalfreedom.plex.util.PlexUtils.tl;

@CommandParameters(description = "Op a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.OP)
public class OpCMD extends PlexCommand
{
    public OpCMD()
    {
        super("op");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        Player player = getNonNullPlayer(args[0]);
        player.setOp(true);
        PlexUtils.broadcast(tl("oppedPlayer", sender.getName(), player.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}