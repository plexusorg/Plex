package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.unmute")
@CommandParameters(name = "unmute", description = "Unmute a player", usage = "/<command> <player>", aliases = "eunmute")
public class UnmuteCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            return usage();
        }
        PlexPlayer punishedPlayer = plugin.getPlayerService().getPlayer(args[0]);
        if (punishedPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        if (!punishedPlayer.isMuted())
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotMuted"));
        }
        punishedPlayer.setMuted(false);
        punishedPlayer.getPunishments().stream().filter(punishment -> punishment.getType() == PunishmentType.MUTE && punishment.isActive()).forEach(punishment ->
        {
            punishment.setActive(false);
            plugin.getPunishmentRepository().updatePunishment(punishment.getType(), false, punishment.getPunished());
        });
        PlexUtils.broadcast(messageComponent("unmutedPlayer", sender.getName(), punishedPlayer.getName()));
        return null;
    }

}
