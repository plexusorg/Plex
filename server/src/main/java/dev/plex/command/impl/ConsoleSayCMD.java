package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.consolesay", source = RequiredCommandSource.CONSOLE)
@CommandParameters(name = "consolesay", usage = "/<command> <message>", description = "Displays a message to everyone", aliases = "csay")
public class ConsoleSayCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        PlexUtils.broadcast(PlexUtils.messageComponent("consoleSayMessage", sender.getName(), PlexUtils.mmStripColor(StringUtils.join(args, " "))));
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return Collections.emptyList();
    }
}