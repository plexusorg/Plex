package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.source.CommandSource;
import java.util.List;
import dev.plex.command.PlexCommand;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.entity.Player;

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