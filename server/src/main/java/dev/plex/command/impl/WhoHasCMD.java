package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;

import java.util.Arrays;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.whohas")
@CommandParameters(name = "whohas", description = "Returns a list of players with a specific item in their inventory.", usage = "/<command> <material>", aliases = "wh")
public class WhoHasCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("material")
                .suggests(suggest(() -> Arrays.stream(Material.values()).map(Enum::name).toList()))
                .executes(context -> executeCommand(context, string(context, "material")))
                .then(literal("clear")
                        .executes(context -> executeCommand(context, string(context, "material"), "clear"))));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        final Material material = Material.getMaterial(args[0].toUpperCase());

        if (material == null)
        {
            return messageComponent("materialNotFound", args[0]);
        }

        boolean clearInventory = args.length > 1 && args[1].equalsIgnoreCase("clear");

        if (clearInventory && !sender.hasPermission("plex.whohas.clear"))
        {
            return messageComponent("noPermissionNode", "plex.whohas.clear");
        }

        List<TextComponent> players = Bukkit.getOnlinePlayers().stream().filter(player ->
                player.getInventory().contains(material)).map(player ->
        {
            if (clearInventory)
            {
                player.getInventory().remove(material);
                player.updateInventory();
            }
            return Component.text(player.getName());
        }).toList();

        return players.isEmpty() ?
                messageComponent("nobodyHasThatMaterial") :
                (clearInventory ?
                        messageComponent("playersMaterialCleared", Component.text(material.name()),
                                Component.join(JoinConfiguration.commas(true), players)) :
                        messageComponent("playersWithMaterial", Component.text(material.name()),
                                Component.join(JoinConfiguration.commas(true), players)));
    }

}
