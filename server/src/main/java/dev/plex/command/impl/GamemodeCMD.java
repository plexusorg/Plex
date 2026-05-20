package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.util.PlexUtils;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GamemodeCMD extends ServerCommand
{
    public GamemodeCMD()
    {
        super(command("gamemode")
            .description("Change your gamemode")
            .usage("/<command> <creative | survival | adventure | default | spectator> [player]")
            .aliases("gm,egamemode,gmt,egmt")
            .permission("plex.gamemode")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        addMode(command, "survival", "s", "0");
        addMode(command, "creative", "c", "1");
        addMode(command, "adventure", "a", "2");
        addMode(command, "default", "d", "5");
        addMode(command, "spectator", "sp", "3", "6");
    }

    private void addMode(LiteralArgumentBuilder<CommandSourceStack> command, String mode, String... aliases)
    {
        command.then(modeNode(mode));
        for (String alias : aliases)
        {
            command.then(modeNode(alias));
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> modeNode(String mode)
    {
        return literal(mode)
                .executes(context -> executeCommand(context, mode))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context, mode, string(context, "player"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }
        switch (args[0].toLowerCase())
        {
            case "survival", "s", "0" ->
            {
                update(context, sender, playerSender, args, GameMode.SURVIVAL);
                return null;
            }
            case "creative", "c", "1" ->
            {
                update(context, sender, playerSender, args, GameMode.CREATIVE);
                return null;
            }
            case "adventure", "a", "2" ->
            {
                update(context, sender, playerSender, args, GameMode.ADVENTURE);
                return null;
            }
            case "default", "d", "5" ->
            {
                update(context, sender, playerSender, args, plugin.getServer().getDefaultGameMode());
                return null;
            }
            case "spectator", "sp", "3", "6" ->
            {
                context.checkPermission(sender, "plex.gamemode.spectator");
                update(context, sender, playerSender, args, GameMode.SPECTATOR);
                return null;
            }
        }
        return context.usage();
    }

    private void update(ServerCommandContext context, CommandSender sender, Player playerSender, String[] args, GameMode gameMode)
    {
        if (args.length > 1)
        {
            context.checkPermission(sender, "plex.gamemode.others");
            Player player = context.getNonNullPlayer(args[1]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, player, gameMode));
            return;
        }
        if (context.isConsole(sender))
        {
            throw new CommandFailException(context.messageString("consoleMustDefinePlayer"));
        }
        if (!(playerSender == null))
        {
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender.getPlayer(), gameMode));
        }
    }

}
