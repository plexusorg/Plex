package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.PlexLog;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class UnbanCMD extends ServerCommand
{
    public UnbanCMD()
    {
        super(command("unban")
            .description("Unbans a player, offline or online")
            .usage("/<command> <player>")
            .permission("plex.ban")
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

        if (args.length == 1)
        {
            PlexPlayer target = plugin.getPlayerService().getPlayer(args[0]);

            if (target == null)
            {
                throw new PlayerNotFoundException();
            }

            plugin.getPunishmentManager().isBanned(target.getUuid()).whenComplete((aBoolean, throwable) ->
            {
                if (throwable != null)
                {
                    plugin.getApi().scheduler().runGlobal(() -> context.send(sender, Component.text("Unable to check ban state.")));
                    return;
                }
                if (!aBoolean)
                {
                    plugin.getApi().scheduler().runGlobal(() -> context.send(sender, context.messageComponent("playerNotBanned")));
                    return;
                }
                plugin.getPunishmentManager().unban(target.getUuid()).whenComplete((unused, failure) -> plugin.getApi().scheduler().runGlobal(() ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to unban {0}: {1}", target.getUuid(), failure.getMessage());
                        context.send(sender, Component.text("Unable to persist the unban; no action was taken."));
                    }
                    else PlexUtils.broadcast(context.messageComponent("unbanningPlayer", context.senderName(), target.getName()));
                }));
            });
        }
        return null;
    }

}
