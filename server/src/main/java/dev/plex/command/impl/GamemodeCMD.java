package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.util.PlexUtils;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "gamemode", usage = "/<command> <creative | survival | adventure | default | spectator> [player]", description = "Change your gamemode", aliases = "gm,egamemode,gmt,egmt")
@CommandPermissions(permission = "plex.gamemode", source = RequiredCommandSource.ANY)
public class GamemodeCMD extends ServerCommand
{
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
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }
        switch (args[0].toLowerCase())
        {
            case "survival", "s", "0" ->
            {
                update(sender, playerSender, args, GameMode.SURVIVAL);
                return null;
            }
            case "creative", "c", "1" ->
            {
                update(sender, playerSender, args, GameMode.CREATIVE);
                return null;
            }
            case "adventure", "a", "2" ->
            {
                update(sender, playerSender, args, GameMode.ADVENTURE);
                return null;
            }
            case "default", "d", "5" ->
            {
                update(sender, playerSender, args, plugin.getServer().getDefaultGameMode());
                return null;
            }
            case "spectator", "sp", "3", "6" ->
            {
                checkPermission(sender, "plex.gamemode.spectator");
                update(sender, playerSender, args, GameMode.SPECTATOR);
                return null;
            }
        }
        return usage();
    }

    private void update(CommandSender sender, Player playerSender, String[] args, GameMode gameMode)
    {
        if (args.length > 1)
        {
            checkPermission(sender, "plex.gamemode.others");
            Player player = getNonNullPlayer(args[1]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, player, gameMode));
            return;
        }
        if (isConsole(sender))
        {
            throw new CommandFailException(PlexUtils.messageString("consoleMustDefinePlayer"));
        }
        if (!(playerSender == null))
        {
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender.getPlayer(), gameMode));
        }
    }

}
