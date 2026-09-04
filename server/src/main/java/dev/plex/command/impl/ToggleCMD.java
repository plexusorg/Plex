package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.menu.dialog.ToggleDialog;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ToggleCMD extends ServerCommand
{
    public ToggleCMD()
    {
        super(command("toggle")
            .description("Allows toggling various server aspects through a GUI")
            .aliases("toggles")
            .permission("plex.toggle")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, this::openOrList));
        command.then(literal("explosions")
                .executes(context -> executeCommand(context, commandContext -> toggle(commandContext, "explosions"))));
        command.then(literal("fluidspread")
                .executes(context -> executeCommand(context, commandContext -> toggle(commandContext, "fluidspread"))));
        command.then(literal("drops")
                .executes(context -> executeCommand(context, commandContext -> toggle(commandContext, "drops"))));
        command.then(literal("redstone")
                .executes(context -> executeCommand(context, commandContext -> toggle(commandContext, "redstone"))));
        command.then(literal("pvp")
                .executes(context -> executeCommand(context, commandContext -> toggle(commandContext, "pvp"))));
        command.then(literal("chat")
                .executes(context -> executeCommand(context, this::toggleChat)));
    }

    private Component toggleChat(ServerCommandContext context)
    {
        PlexUtils.broadcast(PlexUtils.messageComponent("chatToggled", placeholder("sender", context.senderName()), placeholder("state", PlexUtils.messageString(plugin.toggles.getBoolean("chat") ? "stateOff" : "stateOn"))));
        return toggle(context, "chat");
    }

    private Component openOrList(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        if (context.isConsole(sender) || playerSender == null)
        {
            sender.sendMessage(PlexUtils.messageComponent("toggleAvailable"));
            sender.sendMessage(toggleListItem(context, "toggleExplosions", "explosions"));
            sender.sendMessage(toggleListItem(context, "toggleFluidSpread", "fluidspread"));
            sender.sendMessage(toggleListItem(context, "toggleDrops", "drops"));
            sender.sendMessage(toggleListItem(context, "toggleRedstone", "redstone"));
            sender.sendMessage(toggleListItem(context, "togglePvp", "pvp"));
            sender.sendMessage(toggleListItem(context, "toggleChat", "chat"));
            return null;
        }
        new ToggleDialog(plugin).open(playerSender);
        return null;
    }

    private Component toggleListItem(ServerCommandContext context, String nameKey, String toggle)
    {
        return PlexUtils.messageComponent("toggleListItem", placeholder("toggle", PlexUtils.messageString(nameKey)), placeholder("status", status(context, toggle)));
    }

    private Component toggle(ServerCommandContext context, String toggle)
    {
        plugin.toggles.set(toggle, !plugin.getToggles().getBoolean(toggle));
        plugin.toggles.save();
        return PlexUtils.messageComponent("toggleCommandResult", placeholder("toggle", PlexUtils.messageString(toggleNameKey(toggle))), placeholder("status", status(context, toggle)));
    }

    private String status(ServerCommandContext context, String toggle)
    {
        return PlexUtils.messageString(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled");
    }

    private String toggleNameKey(String toggle)
    {
        return switch (toggle)
        {
            case "explosions" -> "toggleExplosions";
            case "fluidspread" -> "toggleFluidSpread";
            case "drops" -> "toggleDrops";
            case "redstone" -> "toggleRedstone";
            case "pvp" -> "togglePvp";
            case "chat" -> "toggleChat";
            default -> toggle;
        };
    }
}
