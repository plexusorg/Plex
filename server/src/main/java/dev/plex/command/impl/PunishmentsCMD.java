package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.impl.PunishedPlayerMenu;
import dev.plex.menu.impl.PunishmentMenu;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "punishments", usage = "/<command> [player]", description = "Opens the Punishments GUI", aliases = "punishlist,punishes")
@CommandPermissions(permission = "plex.punishments", source = RequiredCommandSource.IN_GAME)
public class PunishmentsCMD extends ServerCommand
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
        if (args.length == 0)
        {
            new PunishmentMenu(plugin.getPlayerService()).open(playerSender);
        }
        else
        {
            if (!plugin.getPlayerService().hasPlayedBefore(args[0]))
            {
                throw new PlayerNotFoundException();
            }

            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[0]);
            final PlexPlayer player = offlinePlayer.isOnline() ? getOnlinePlexPlayer(args[0]) : getOfflinePlexPlayer(offlinePlayer.getUniqueId());

            new PunishedPlayerMenu(player, plugin.getPlayerService()).open(playerSender);
        }

        return null;
    }

}
