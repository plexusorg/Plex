package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "deop", description = "Deop a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.deop")
public class DeopCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        Player player = getNonNullPlayer(args[0]);
        player.setOp(false);
        PlexUtils.broadcast(tl("oppedPlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}