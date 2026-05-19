package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.impl.ToggleMenu;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "toggle", description = "Allows toggling various server aspects through a GUI", aliases = "toggles")
@CommandPermissions(permission = "plex.toggle", source = RequiredCommandSource.ANY)
public class ToggleCMD extends ServerCommand
{
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
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (isConsole(sender) || playerSender == null)
        {
            if (args.length == 0)
            {
                sender.sendMessage(messageComponent("toggleAvailable"));
                sender.sendMessage(toggleListItem("toggleExplosions", "explosions"));
                sender.sendMessage(toggleListItem("toggleFluidSpread", "fluidspread"));
                sender.sendMessage(toggleListItem("toggleDrops", "drops"));
                sender.sendMessage(toggleListItem("toggleRedstone", "redstone"));
                sender.sendMessage(toggleListItem("togglePvp", "pvp"));
                sender.sendMessage(toggleListItem("toggleChat", "chat"));
                return null;
            }
            switch (args[0].toLowerCase())
            {
                case "explosions" ->
                {
                    return toggle("explosions");
                }
                case "fluidspread" ->
                {
                    return toggle("fluidspread");
                }
                case "drops" ->
                {
                    return toggle("drops");
                }
                case "redstone" ->
                {
                    return toggle("redstone");
                }
                case "pvp" ->
                {
                    return toggle("pvp");
                }
                case "chat" ->
                {
                    PlexUtils.broadcast(PlexUtils.messageComponent("chatToggled", sender.getName(), messageString(plugin.toggles.getBoolean("chat") ? "stateOff" : "stateOn")));
                    return toggle("chat");
                }
                default ->
                {
                    return messageComponent("invalidToggle");
                }
            }
        }
        new ToggleMenu(plugin).open(playerSender);
        return null;
    }

    private Component toggleListItem(String nameKey, String toggle)
    {
        return messageComponent("toggleListItem", messageString(nameKey), status(toggle));
    }

    private Component toggle(String toggle)
    {
        plugin.toggles.set(toggle, !plugin.getToggles().getBoolean(toggle));
        return messageComponent("toggleCommandResult", messageString(toggleNameKey(toggle)), status(toggle));
    }

    private String status(String toggle)
    {
        return messageString(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled");
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
