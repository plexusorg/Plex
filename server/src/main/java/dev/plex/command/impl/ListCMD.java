package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.VaultHook;
import dev.plex.meta.PlayerMeta;
import dev.plex.util.PlexUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        command.executes(context -> executeCommand(context,
                commandContext -> executeTyped(commandContext, ListMode.DEFAULT)));
        command.then(literal("-d")
                .executes(context -> executeCommand(context,
                        commandContext -> executeTyped(commandContext, ListMode.DISPLAY_NAMES))));
        command.then(literal("-v")
                .requires(source -> canUsePermission(source, "plex.list.vanished"))
                .executes(context -> executeCommand(context,
                        commandContext -> executeTyped(commandContext, ListMode.VANISHED))));
    }

    private Component executeTyped(ServerCommandContext context, ListMode mode)
    {
        CommandSender sender = context.sender();
        List<CompletableFuture<ListedPlayer>> captures = plugin.getPlayerService().cachedPlayers().stream()
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(Objects::nonNull)
                .map(this::capture)
                .toList();
        int maxPlayers = Bukkit.getMaxPlayers();
        CompletableFuture.allOf(captures.toArray(CompletableFuture[]::new)).thenRun(() ->
        {
            List<ListedPlayer> players = captures.stream().map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .filter(player -> mode == ListMode.VANISHED ? player.vanished() : !player.vanished())
                    .toList();
            sender.sendMessage(PlexUtils.messageComponent(players.size() == 1 ? "listHeader" : "listHeaderPlural",
                    placeholder("online_count", players.size()), placeholder("total_count", maxPlayers)));
            if (!players.isEmpty()) sender.sendMessage(playerList(players, mode));
        });
        return null;
    }

    private CompletableFuture<ListedPlayer> capture(Player player)
    {
        CompletableFuture<ListedPlayer> result = new CompletableFuture<>();
        boolean scheduled = player.getScheduler().execute(plugin, () ->
        {
            var cachedPlayer = plugin.getPlayerService().cachedPlayer(player.getUniqueId());
            if (cachedPlayer == null)
            {
                result.complete(null);
                return;
            }
            Component prefix = VaultHook.getPrefix(cachedPlayer);
            result.complete(new ListedPlayer(prefix, player.getName(),
                    player.displayName(), PlayerMeta.isVanished(player)));
        }, () -> result.complete(null), 0L);
        if (!scheduled) result.complete(null);
        return result;
    }

    private Component playerList(List<ListedPlayer> players, ListMode mode)
    {
        Component list = Component.empty();
        for (int i = 0; i < players.size(); i++)
        {
            ListedPlayer player = players.get(i);
            Component prefix = player.prefix();
            if (!List.of(Component.empty(), Component.space()).contains(prefix))
            {
                list = list.append(prefix).append(Component.space());
            }
            list = list.append(Component.text(player.name()).color(NamedTextColor.WHITE));
            if (mode == ListMode.DISPLAY_NAMES)
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

    private record ListedPlayer(Component prefix, String name, Component displayName, boolean vanished) {}

    private enum ListMode
    {
        DEFAULT,
        DISPLAY_NAMES,
        VANISHED
    }

    }
