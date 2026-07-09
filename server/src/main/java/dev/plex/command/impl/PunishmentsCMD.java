package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.dialog.PunishmentDialog;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PunishmentsCMD extends ServerCommand
{
    public PunishmentsCMD()
    {
        super(command("punishments")
            .description("Opens the Punishments GUI")
            .usage("/<command> [player]")
            .aliases("punishlist,punishes")
            .permission("plex.punishments")
            .source(RequiredCommandSource.IN_GAME)
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
        PunishmentDialog dialog = new PunishmentDialog(plugin.getPlayerService());
        if (args.length == 0)
        {
            dialog.open(playerSender);
        }
        else
        {
            if (!plugin.getPlayerService().hasPlayedBefore(args[0]))
            {
                throw new PlayerNotFoundException();
            }

            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            final PlexPlayer player = offlinePlayer.isOnline() ? context.getOnlinePlexPlayer(args[0]) : context.getOfflinePlexPlayer(offlinePlayer.getUniqueId());

            dialog.open(playerSender, player);
        }

        return null;
    }

}
