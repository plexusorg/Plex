package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandParameters(name = "gamemode", usage = "/<command> <creative | survival | adventure | default | spectator> [player]", description = "Change your gamemode", aliases = "gm,egamemode,gmt,egmt")
@CommandPermissions(level = Rank.OP, permission = "plex.gamemode", source = RequiredCommandSource.ANY)
public class GamemodeCMD extends PlexCommand
{
    private GameMode gamemode;

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
                gamemode = GameMode.SURVIVAL;
                update(sender, playerSender, GameMode.SURVIVAL);
                return null;
            }
            case "creative", "c", "1" ->
            {
                gamemode = GameMode.CREATIVE;
                update(sender, playerSender, GameMode.CREATIVE);
                return null;
            }
            case "adventure", "a", "2" ->
            {
                gamemode = GameMode.ADVENTURE;
                update(sender, playerSender, GameMode.ADVENTURE);
                return null;
            }
            case "default", "d", "5" ->
            {
                gamemode = plugin.getServer().getDefaultGameMode();
                update(sender, playerSender, plugin.getServer().getDefaultGameMode());
                return null;
            }
            case "spectator", "sp", "3", "6" ->
            {
                gamemode = GameMode.SPECTATOR;
                checkRank(sender, Rank.ADMIN, "plex.gamemode.spectator");
                update(sender, playerSender, GameMode.SPECTATOR);
                return null;
            }
        }
        if (args.length > 1)
        {
            checkRank(sender, Rank.ADMIN, "plex.gamemode.others");
            Player player = getNonNullPlayer(args[1]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, player, gamemode));
        }
        return null;
    }

    private void update(CommandSender sender, Player playerSender, GameMode gameMode)
    {
        if (isConsole(sender))
        {
            throw new CommandFailException(PlexUtils.messageString("consoleMustDefinePlayer"));
        }
        if (!(playerSender == null))
        {
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender.getPlayer(), gameMode));
        }
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return Arrays.asList("creative", "survival", "adventure", "spectator", "default");
        }
        if (args.length == 2)
        {
            return PlexUtils.getPlayerNameList();
        }
        return Collections.emptyList();
    }
}