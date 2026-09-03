package dev.plex.command.impl;

import org.bukkit.Bukkit;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.dialog.PunishmentDialog;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PunishmentsCMD extends ServerCommand
{
    public PunishmentsCMD()
    {
        super(command("punishments")
            .description("Opens the Punishments GUI")
            .usage("/<command> [player]")
            .aliases("punishlist,punishes")
            .permission("plex.punishments")
            .source(RequiredCommandSource.IN_GAME)
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, null)));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "player")))));
    }

    private Component executeTyped(ServerCommandContext context, String playerName)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        PunishmentDialog dialog = new PunishmentDialog(plugin, plugin.getPlayerService());
        if (playerName == null)
        {
            dialog.open(playerSender);
        }
        else
        {
            plugin.getPlayerService().findPlayer(playerName).whenComplete((player, failure) ->
            {
                if (failure != null) playerSender.sendMessage(Component.text("Unable to load the player's punishments."));
                else if (player == null) playerSender.sendMessage(PlexUtils.messageComponent("playerNotFound"));
                else dialog.open(playerSender, player);
            });
        }

        return null;
    }

}
