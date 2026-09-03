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

public class CreativeCMD extends ServerCommand
{
    public CreativeCMD()
    {
        super(command("creative")
            .description("Set your own or another player's gamemode to creative mode")
            .aliases("gmc,egmc,ecreative,eecreative,creativemode,ecreativemode")
            .permission("plex.gamemode.creative")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, null)));
        command.then(word("target")
                .requires(source -> canUsePermission(source, "plex.gamemode.creative.others"))
                .suggests(suggestPlayersAndAll("plex.gamemode.creative.others"))
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "target")))));
    }

    private Component executeTyped(ServerCommandContext context, String target)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        if (target == null)
        {
            if (context.isConsole(sender))
            {
                throw new CommandFailException(PlexUtils.messageString("consoleMustDefinePlayer"));
            }
            if (!(playerSender == null))
            {
                GameModeUtil.update(plugin, sender, playerSender, GameMode.CREATIVE);
            }
            return null;
        }

        context.checkPermission(sender, "plex.gamemode.creative.others");
        if (target.equals("-a"))
        {
            GameModeUtil.updateAll(plugin, GameMode.CREATIVE, false).thenRun(() ->
                    PlexUtils.broadcast(PlexUtils.messageComponent("setEveryoneGameMode", context.senderName(), "creative")));
            return null;
        }

        Player nPlayer = getNonNullPlayer(target);
        GameModeUtil.update(plugin, sender, nPlayer, GameMode.CREATIVE);
        return null;
    }

}
