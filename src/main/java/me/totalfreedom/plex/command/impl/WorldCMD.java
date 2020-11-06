package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.CommandArgumentException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.IN_GAME)
@CommandParameters(description = "Teleport to a world.", usage = "/<command> <world>")
public class WorldCMD extends PlexCommand
{
    public WorldCMD()
    {
        super("world");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        World world = getNonNullWorld(args[0]);
        sender.getPlayer().teleport(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0, 0, 0));
        send(tl("playerWorldTeleport", world.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds())
        {
            worlds.add(world.getName());
        }
        if (args.length == 1)
        {
            return worlds;
        }
        return ImmutableList.of();
    }

}
