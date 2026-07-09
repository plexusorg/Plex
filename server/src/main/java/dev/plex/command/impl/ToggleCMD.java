package dev.plex.command.impl;

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
        command.executes(context -> executeCommand(context));
        command.then(literal("explosions")
                .executes(context -> executeCommand(context, "explosions")));
        command.then(literal("fluidspread")
                .executes(context -> executeCommand(context, "fluidspread")));
        command.then(literal("drops")
                .executes(context -> executeCommand(context, "drops")));
        command.then(literal("redstone")
                .executes(context -> executeCommand(context, "redstone")));
        command.then(literal("pvp")
                .executes(context -> executeCommand(context, "pvp")));
        command.then(literal("chat")
                .executes(context -> executeCommand(context, "chat")));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length > 0)
        {
            switch (args[0].toLowerCase())
            {
                case "explosions" ->
                {
                    return toggle(context, "explosions");
                }
                case "fluidspread" ->
                {
                    return toggle(context, "fluidspread");
                }
                case "drops" ->
                {
                    return toggle(context, "drops");
                }
                case "redstone" ->
                {
                    return toggle(context, "redstone");
                }
                case "pvp" ->
                {
                    return toggle(context, "pvp");
                }
                case "chat" ->
                {
                    PlexUtils.broadcast(PlexUtils.messageComponent("chatToggled", sender.getName(), context.messageString(plugin.toggles.getBoolean("chat") ? "stateOff" : "stateOn")));
                    return toggle(context, "chat");
                }
                default ->
                {
                    return context.messageComponent("invalidToggle");
                }
            }
        }
        if (context.isConsole(sender) || playerSender == null)
        {
            sender.sendMessage(context.messageComponent("toggleAvailable"));
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
        return context.messageComponent("toggleListItem", context.messageString(nameKey), status(context, toggle));
    }

    private Component toggle(ServerCommandContext context, String toggle)
    {
        plugin.toggles.set(toggle, !plugin.getToggles().getBoolean(toggle));
        return context.messageComponent("toggleCommandResult", context.messageString(toggleNameKey(toggle)), status(context, toggle));
    }

    private String status(ServerCommandContext context, String toggle)
    {
        return context.messageString(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled");
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
