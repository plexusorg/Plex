package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import org.bukkit.Bukkit;

import dev.plex.util.PlexUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WhoHasCMD extends ServerCommand
{
    public WhoHasCMD()
    {
        super(command("whohas")
            .description("Returns a list of players with a specific item in their inventory.")
            .usage("/<command> <material>")
            .aliases("wh")
            .permission("plex.whohas")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(word("material")
                .suggests(suggest(() -> Arrays.stream(Material.values()).map(Enum::name).toList()))
                .executes(context -> executeCommand(context,
                        commandContext -> executeTyped(commandContext, string(context, "material"), false)))
                .then(literal("clear")
                        .executes(context -> executeCommand(context,
                                commandContext -> executeTyped(commandContext, string(context, "material"), true)))));
    }

    private Component executeTyped(ServerCommandContext context, String materialName, boolean clearInventory)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        final Material material = Material.getMaterial(materialName.toUpperCase());

        if (material == null)
        {
            return PlexUtils.messageComponent("materialNotFound", placeholder("material", materialName));
        }

        if (clearInventory && !sender.hasPermission("plex.whohas.clear"))
        {
            return PlexUtils.messageComponent("noPermissionNode", placeholder("permission", "plex.whohas.clear"));
        }

        List<CompletableFuture<TextComponent>> captures = plugin.getPlayerService().cachedPlayers().stream()
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(Objects::nonNull)
                .map(player -> capture(player, material, clearInventory))
                .toList();
        CompletableFuture.allOf(captures.toArray(CompletableFuture[]::new)).thenRun(() ->
        {
            List<TextComponent> players = captures.stream().map(CompletableFuture::join)
                    .filter(Objects::nonNull).toList();
            sender.sendMessage(result(material, clearInventory, players));
        });
        return null;
    }

    private CompletableFuture<TextComponent> capture(Player player, Material material, boolean clearInventory)
    {
        CompletableFuture<TextComponent> result = new CompletableFuture<>();
        boolean scheduled = player.getScheduler().execute(plugin, () ->
        {
            if (!player.getInventory().contains(material))
            {
                result.complete(null);
                return;
            }
            if (clearInventory)
            {
                player.getInventory().remove(material);
                player.updateInventory();
            }
            result.complete(Component.text(player.getName()));
        }, () -> result.complete(null), 0L);
        if (!scheduled) result.complete(null);
        return result;
    }

    private Component result(Material material, boolean clearInventory, List<TextComponent> players)
    {
        return players.isEmpty() ?
                PlexUtils.messageComponent("nobodyHasThatMaterial") :
                (clearInventory ?
                        PlexUtils.messageComponent("playersMaterialCleared", placeholder("material", Component.text(material.name())), placeholder("players", Component.join(JoinConfiguration.commas(true), players))) :
                        PlexUtils.messageComponent("playersWithMaterial", placeholder("material", Component.text(material.name())), placeholder("players", Component.join(JoinConfiguration.commas(true), players))));
    }

}
