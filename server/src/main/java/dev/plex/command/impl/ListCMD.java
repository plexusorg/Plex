package dev.plex.command.impl;

import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.hook.VaultHook;
import dev.plex.meta.PlayerMeta;
import dev.plex.util.PlexUtils;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "list", description = "Show a list of all online players", usage = "/<command> [-d | -v]", aliases = "lsit,who,playerlist,online")
@CommandPermissions(permission = "plex.list")
public class ListCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());
        if (args.length > 0 && args[0].equalsIgnoreCase("-v"))
        {
            checkPermission(sender, "plex.list.vanished");
            players.removeIf(player -> !PlayerMeta.isVanished(player));
        }
        else
        {
            players.removeIf(PlayerMeta::isVanished);
        }
        Component list = Component.empty();
        Component header = PlexUtils.messageComponent(players.size() == 1 ? "listHeader" : "listHeaderPlural", players.size(), Bukkit.getMaxPlayers());
        send(sender, header);
        if (players.isEmpty())
        {
            return null;
        }
        for (int i = 0; i < players.size(); i++)
        {
            Player player = players.get(i);
            Component prefix = VaultHook.getPrefix(getPlexPlayer(player));
            if (prefix != null && !prefix.equals(Component.empty()) && !prefix.equals(Component.space()))
            {
                list = list.append(prefix).append(Component.space());
            }
            list = list.append(Component.text(player.getName()).color(NamedTextColor.WHITE));
            if (args.length > 0 && args[0].equalsIgnoreCase("-d"))
            {
                list = list.append(Component.space());
                list = list.append(Component.text("(").color(NamedTextColor.WHITE));
                list = list.append(player.displayName());
                list = list.append(Component.text(")").color(NamedTextColor.WHITE));
            }
            if (i != players.size() - 1)
            {
                list = list.append(Component.text(",")).append(Component.space());
            }
        }
        return list;
    }

    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return List.of("-d", "-v");
        }
        return Collections.emptyList();
    }
}
