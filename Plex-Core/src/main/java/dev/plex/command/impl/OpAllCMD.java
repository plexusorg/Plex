package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.annotation.System;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "opall", description = "Op everyone on the server", aliases = "opa")
@CommandPermissions(level = Rank.ADMIN)
@System(value = "ranks")
public class OpAllCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        for (Player player : Bukkit.getOnlinePlayers())
        {
            player.setOp(true);
        }
        PlexUtils.broadcast(messageComponent("oppedAllPlayers", sender.getName()));
        return null;
    }
}