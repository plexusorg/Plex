package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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