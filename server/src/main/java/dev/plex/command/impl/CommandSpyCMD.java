package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.commandspy", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "commandspy", aliases = "cmdspy", description = "Spy on other player's commands")
public class CommandSpyCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (playerSender != null)
        {
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            plexPlayer.setCommandSpy(!plexPlayer.isCommandSpy());
            DataUtils.update(plexPlayer);
            send(sender, messageComponent("toggleCommandSpy")
                    .append(Component.space())
                    .append(plexPlayer.isCommandSpy() ? messageComponent("enabled") : messageComponent("disabled")));
        }
        return null;
    }
}
