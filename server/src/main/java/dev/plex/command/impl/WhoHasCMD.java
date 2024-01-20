package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@CommandPermissions(permission = "plex.whohas")
@CommandParameters(name = "whohas", description = "Returns a list of players with a specific item in their inventory.", usage = "/<command> <material>", aliases = "wh")
public class WhoHasCMD extends PlexCommand
{
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

        final List<TextComponent> players = Bukkit.getOnlinePlayers().stream().filter(player ->
                player.getInventory().contains(material)).map(player -> Component.text(player.getName())).toList();

        return players.isEmpty() ? messageComponent("nobodyHasThatMaterial") :
                messageComponent("playersWithMaterial", Component.text(material.name()),
                        Component.join(JoinConfiguration.commas(true), players));
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? Arrays.stream(Material.values()).map(Enum::name).toList() : ImmutableList.of();
    }
}
