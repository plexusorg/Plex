package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.PlexLog;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnfreezeCMD extends ServerCommand
{
    public UnfreezeCMD()
    {
        super(command("unfreeze")
            .description("Unfreeze a player")
            .usage("/<command> <player>")
            .permission("plex.unfreeze")
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

        if (!plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.FREEZE))
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotFrozen"));
        }
        plugin.getPunishmentManager().deactivateTimedPunishment(punishedPlayer, PunishmentType.FREEZE)
                .whenComplete((unused, failure) ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to unfreeze {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                        context.send(sender, Component.text("Unable to persist the unfreeze; no action was taken."));
                    }
                    else PlexUtils.broadcast(context.messageComponent("unfrozePlayer", context.senderName(), punishedPlayer.getName()));
                });
        return null;
    }

}
