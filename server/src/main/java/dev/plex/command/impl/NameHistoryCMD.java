package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import dev.plex.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@CommandParameters(name = "namehistory", description = "Get the name history of a player", usage = "/<command> <player>", aliases = "nh")
@CommandPermissions(level = Rank.OP, permission = "plex.namehistory")
public class NameHistoryCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            return usage();
        }
        String username = args[0];

        AshconInfo info = MojangUtils.getInfo(username);
        if (info == null)
        {
            return messageComponent("nameHistoryDoesntExist");
        }
        PlexLog.debug("NameHistory UUID: " + info.getUuid());
        PlexLog.debug("NameHistory Size: " + info.getUsernameHistories().length);
        List<Component> historyList = Lists.newArrayList();
        Arrays.stream(info.getUsernameHistories()).forEach(history ->
        {
            if (history.getZonedDateTime() != null)
            {
                historyList.add(
                        messageComponent("nameHistoryBody",
                                history.getUsername(),
                                TimeUtils.useTimezone(history.getZonedDateTime())));
            }
            else
            {
                historyList.add(
                        Component.text(history.getUsername()).color(NamedTextColor.GOLD)
                                .append(Component.space()));
            }
        });
        send(sender, messageComponent("nameHistoryTitle", username));
        send(sender, messageComponent("nameHistorySeparator"));
        historyList.forEach(component -> send(sender, component));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}