package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.masterbuilderworld", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "masterbuilderworld", aliases = "mbw", description = "Teleport to the Master Builder world")
public class MasterbuilderworldCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        assert playerSender != null;
        // TODO: Add masterbuilderworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("masterbuilderworld"), 0, 50, 0);
            playerSender.teleportAsync(loc);
            return messageComponent("teleportedToWorld", "Master Builder World");
        }
        return null;
    }

}
