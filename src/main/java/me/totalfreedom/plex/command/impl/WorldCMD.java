package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotations.CommandParameters;
import me.totalfreedom.plex.command.annotations.CommandPermissions;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static me.totalfreedom.plex.util.PlexUtils.tl;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.IN_GAME)
@CommandParameters(description = "Teleport to a world.", usage = "/<command> <world>")
public class WorldCMD extends PlexCommand
{
    public WorldCMD() {
        super("world");
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        Player player = (Player) sender;
        World world = Bukkit.getWorld(args[0]);
        player.teleport(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0, 0, 0));
        sender.sendMessage(tl("playerWorldTeleport", world.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args)
    {
        List<String> worlds = new ArrayList<>();
        for (World world : Bukkit.getWorlds())
            worlds.add(world.getName());
        if (args.length == 1)
            return worlds;
        return ImmutableList.of();
    }

}
