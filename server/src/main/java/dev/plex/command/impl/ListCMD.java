package dev.plex.command.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.VaultHook;
import dev.plex.meta.PlayerMeta;
import dev.plex.util.PlexUtils;

import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ListCMD extends ServerCommand
{
    public ListCMD()
    {
        super(command("list")
            .description("Show a list of all online players")
            .usage("/<command> [-d | -v]")
            .aliases("lsit,who,playerlist,online")
            .permission("plex.list")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("-d")
                .executes(context -> executeCommand(context, "-d")));
        command.then(literal("-v")
                .requires(source -> canUsePermission(source, "plex.list.vanished"))
                .executes(context -> executeCommand(context, "-v")));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        List<Player> players = Lists.newArrayList(Bukkit.getOnlinePlayers());
        if (args.length > 0 && args[0].equalsIgnoreCase("-v"))
        {
            context.checkPermission(sender, "plex.list.vanished");
            players.removeIf(player -> !PlayerMeta.isVanished(player));
        }
        else
        {
            players.removeIf(PlayerMeta::isVanished);
        }
        Component list = Component.empty();
        Component header = PlexUtils.messageComponent(players.size() == 1 ? "listHeader" : "listHeaderPlural", players.size(), Bukkit.getMaxPlayers());
        context.send(sender, header);
        if (players.isEmpty())
        {
            return null;
        }
        for (int i = 0; i < players.size(); i++)
        {
            Player player = players.get(i);
            Component prefix = VaultHook.getPrefix(context.getPlexPlayer(player));
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

    }
