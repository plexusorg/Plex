package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AdminworldCMD extends ServerCommand
{
    public AdminworldCMD()
    {
        super(command("adminworld")
            .description("Teleport to the adminworld")
            .aliases("aw")
            .permission("plex.adminworld")
            .source(RequiredCommandSource.IN_GAME)
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        assert playerSender != null;
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("adminworld"), 0, 50, 0);
            playerSender.teleportAsync(loc);
            return context.messageComponent("teleportedToWorld", "adminworld");
        }
        return null;
    }

}
