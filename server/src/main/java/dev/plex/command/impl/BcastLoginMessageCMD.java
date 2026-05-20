package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BcastLoginMessageCMD extends ServerCommand
{
    public BcastLoginMessageCMD()
    {
        super(command("bcastloginmessage")
            .description("Broadcast your login message (for vanish support)")
            .usage("/<command> <player>")
            .aliases("bcastlm")
            .permission("plex.broadcastloginmessage")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
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

        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[0]);

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        String loginMessage = PlayerMeta.getLoginMessage(plexPlayer);
        if (!loginMessage.isEmpty())
        {
            PlexUtils.broadcast(PlexUtils.stringToComponent(loginMessage));
            PlexUtils.broadcast(context.messageComponent("loginMessage", plexPlayer.getName()));
        }
        else
        {
            return context.messageComponent("playerHasNoLoginMessage");
        }

        return null;
    }

}
