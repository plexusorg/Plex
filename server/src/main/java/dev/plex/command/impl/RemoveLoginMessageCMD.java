package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveLoginMessageCMD extends ServerCommand
{
    public RemoveLoginMessageCMD()
    {
        super(command("removeloginmessage")
            .description("Remove your own (or someone else's) login message")
            .usage("/<command> [-o <player>]")
            .aliases("rlm,removeloginmsg")
            .permission("plex.removeloginmessage")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("-o")
                .requires(source -> canUsePermission(source, "plex.removeloginmessage.others"))
                .executes(context -> executeCommand(context, "-o"))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context, "-o", string(context, "player")))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0 && !context.isConsole(sender))
        {
            if (playerSender != null)
            {
                PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
                plexPlayer.setLoginMessage("");
                plugin.getPlayerService().update(plexPlayer);
                return context.messageComponent("removedOwnLoginMessage");
            }
        }
        else if (args[0].equalsIgnoreCase("-o"))
        {
            context.checkPermission(sender, "plex.removeloginmessage.others");

            if (args.length < 2)
            {
                return context.messageComponent("specifyPlayer");
            }

            PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[1]);
            if (plexPlayer == null)
            {
                return context.messageComponent("playerNotFound");
            }
            plexPlayer.setLoginMessage("");
            plugin.getPlayerService().update(plexPlayer);
            return context.messageComponent("removedOtherLoginMessage", plexPlayer.getName());
        }
        else
        {
            return context.messageComponent("noPermissionConsole");
        }
        return null;
    }

}
