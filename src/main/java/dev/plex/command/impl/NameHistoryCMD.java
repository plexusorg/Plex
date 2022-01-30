package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.rank.enums.Rank;
import dev.plex.util.MojangUtils;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandParameters(name = "namehistory", description = "Get the name history of a player", usage = "/<command> <player>", aliases = "nh")
@CommandPermissions(level = Rank.OP, permission = "plex.namehistory")
public class NameHistoryCMD extends PlexCommand
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' HH:mm:ss");

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        String username = args[0];


        UUID uuid;
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(username);
        if (offlinePlayer != null)
        {
            uuid = offlinePlayer.getUniqueId();
        } else
        {
            uuid = MojangUtils.getUUID(username);
        }

        if (uuid == null)
        {
            return Component.text("Couldn't find this user! Please check if your spelling was correct and this player exists").color(NamedTextColor.RED);
        }
        PlexLog.debug("NameHistory UUID: " + uuid);

        List<Map.Entry<String, LocalDateTime>> history = MojangUtils.getNameHistory(uuid);
        PlexLog.debug("NameHistory Size: " + history.size());
        List<Component> historyList = Lists.newArrayList();
        history.forEach(entry ->
        {
            if (entry.getValue() != null)
            {
                historyList.add(
                        Component.text(entry.getKey()).color(NamedTextColor.GOLD)
                                .append(Component.space())
                                .append(Component.text("-").color(NamedTextColor.DARK_GRAY))
                                .append(Component.space())
                                .append(Component.text(DATE_FORMAT.format(entry.getValue())).color(NamedTextColor.GOLD)));
            } else
            {
                historyList.add(
                        Component.text(entry.getKey()).color(NamedTextColor.GOLD)
                                .append(Component.space()));
            }
        });
        send(sender, Component.text("Name History (" + username + ")").color(NamedTextColor.GOLD));
        send(sender, Component.text("-----------------------------").color(NamedTextColor.GOLD).decoration(TextDecoration.STRIKETHROUGH, true));
        historyList.forEach(component -> send(sender, component));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}