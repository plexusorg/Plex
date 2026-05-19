package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.removeloginmessage", source = RequiredCommandSource.ANY)
@CommandParameters(name = "removeloginmessage", usage = "/<command> [-o <player>]", description = "Remove your own (or someone else's) login message", aliases = "rlm,removeloginmsg")
public class RemoveLoginMessageCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("-o")
                .requires(source -> silentCheckPermission(source.getSender(), "plex.removeloginmessage.others"))
                .executes(context -> executeCommand(context, "-o"))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context, "-o", string(context, "player")))));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0 && !isConsole(sender))
        {
            if (playerSender != null)
            {
                PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
                plexPlayer.setLoginMessage("");
                return messageComponent("removedOwnLoginMessage");
            }
        }
        else if (args[0].equalsIgnoreCase("-o"))
        {
            checkPermission(sender, "plex.removeloginmessage.others");

            if (args.length < 2)
            {
                return messageComponent("specifyPlayer");
            }

            PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[1]);
            if (plexPlayer == null)
            {
                return messageComponent("playerNotFound");
            }
            plexPlayer.setLoginMessage("");
            return messageComponent("removedOtherLoginMessage", plexPlayer.getName());
        }
        else
        {
            return messageComponent("noPermissionConsole");
        }
        return null;
    }

}
