package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.util.GameModeUtil;
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
        addMode(command, GameMode.SURVIVAL, false, "survival", "s", "0");
        addMode(command, GameMode.CREATIVE, false, "creative", "c", "1");
        addMode(command, GameMode.ADVENTURE, false, "adventure", "a", "2");
        addMode(command, plugin.getServer().getDefaultGameMode(), false, "default", "d", "5");
        addMode(command, GameMode.SPECTATOR, true, "spectator", "sp", "3", "6");
    }

    private void addMode(LiteralArgumentBuilder<CommandSourceStack> command, GameMode mode,
                         boolean spectatorPermission, String... literals)
    {
        for (String literal : literals)
        {
            command.then(modeNode(literal, mode, spectatorPermission));
        }
    }

    private LiteralArgumentBuilder<CommandSourceStack> modeNode(String literal, GameMode mode, boolean spectatorPermission)
    {
        return literal(literal)
                .executes(context -> executeCommand(context,
                        commandContext -> update(commandContext, mode, null, spectatorPermission)))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context,
                                commandContext -> update(commandContext, mode, string(context, "player"), spectatorPermission))));
    }

    private Component update(ServerCommandContext context, GameMode gameMode, String playerName,
                             boolean spectatorPermission)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        if (spectatorPermission)
        {
            context.checkPermission(sender, "plex.gamemode.spectator");
        }
        if (playerName != null)
        {
            context.checkPermission(sender, "plex.gamemode.others");
            Player player = getNonNullPlayer(playerName);
            GameModeUtil.update(plugin, sender, player, gameMode);
            return null;
        }
        if (context.isConsole(sender))
        {
            throw new CommandFailException(PlexUtils.messageString("consoleMustDefinePlayer"));
        }
        if (!(playerSender == null))
        {
            GameModeUtil.update(plugin, sender, playerSender, gameMode);
        }
        return null;
    }

}
