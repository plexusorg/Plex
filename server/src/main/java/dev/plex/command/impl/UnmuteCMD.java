package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
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

public class UnmuteCMD extends ServerCommand
{
    public UnmuteCMD()
    {
        super(command("unmute")
            .description("Unmute a player")
            .usage("/<command> <player>")
            .aliases("eunmute")
            .permission("plex.unmute")
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
        if (args.length != 1)
        {
            return context.usage();
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
        PlexUtils.broadcast(context.messageComponent("unmutedPlayer", sender.getName(), punishedPlayer.getName()));
        return null;
    }

}
