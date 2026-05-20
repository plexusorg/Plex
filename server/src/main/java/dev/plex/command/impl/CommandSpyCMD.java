package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandSpyCMD extends ServerCommand
{
    public CommandSpyCMD()
    {
        super(command("commandspy")
            .description("Spy on other player's commands")
            .aliases("cmdspy")
            .permission("plex.commandspy")
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
        if (playerSender != null)
        {
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            plexPlayer.setCommandSpy(!plexPlayer.isCommandSpy());
            plugin.getPlayerService().update(plexPlayer);
            context.send(sender, context.messageComponent("toggleCommandSpy")
                    .append(Component.space())
                    .append(plexPlayer.isCommandSpy() ? context.messageComponent("enabled") : context.messageComponent("disabled")));
        }
        return null;
    }

}
