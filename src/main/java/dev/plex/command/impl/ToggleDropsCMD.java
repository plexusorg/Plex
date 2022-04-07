package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.services.ServiceManager;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.toggledrops", source = RequiredCommandSource.ANY)
@CommandParameters(name = "toggledrops", description = "Toggle immediately removing drops.", usage = "/<command>", aliases = "td")
public class ToggleDropsCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        plugin.config.set("allowdrops", !plugin.config.getBoolean("allowdrops"));
        plugin.config.save();
        send(sender, plugin.config.getBoolean("allowdrops") ? messageComponent("allowDropsEnabled") : messageComponent("allowDropsDisabled"));
        return null;
    }
}