package dev.plex.command.impl;

import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import io.papermc.lib.PaperLib;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
@CommandParameters(aliases = "mbw", description = "Teleport to the Master Builder world")
public class MasterbuilderworldCMD extends PlexCommand
{
    public MasterbuilderworldCMD()
    {
        super("masterbuilderworld");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("masterbuilderworld"), 0, 50, 0);
            PaperLib.teleportAsync(sender.getPlayer(), loc);
            send(tl("teleportedToWorld", "Master Builder world"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return Collections.emptyList();
    }
}
