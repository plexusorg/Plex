package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "unban", usage = "/<command> <player>", description = "Unbans a player, offline or online")
@CommandPermissions(permission = "plex.ban", source = RequiredCommandSource.ANY)

public class UnbanCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

    @Override
    public Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args.length == 1)
        {
            PlexPlayer target = plugin.getPlayerService().getPlayer(args[0]);

            if (target == null)
            {
                throw new PlayerNotFoundException();
            }

            plugin.getPunishmentManager().isAsyncBanned(target.getUuid()).whenComplete((aBoolean, throwable) ->
            {
                if (!aBoolean)
                {
                    send(sender, messageComponent("playerNotBanned"));
                    return;
                }
                plugin.getPunishmentManager().unban(target.getUuid());
                PlexUtils.broadcast(messageComponent("unbanningPlayer", sender.getName(), target.getName()));
            });
        }
        return null;
    }

}
