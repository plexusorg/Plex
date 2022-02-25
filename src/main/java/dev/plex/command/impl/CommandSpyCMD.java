package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            PlexPlayer plexPlayer = DataUtils.getPlayer(playerSender.getUniqueId());
            plexPlayer.setCommandSpy(!plexPlayer.isCommandSpy());
            DataUtils.update(plexPlayer);
            return Component.text(PlexUtils.messageString("toggleCommandSpy")).color(NamedTextColor.GRAY)
                    .append(Component.space())
                    .append(Component.text(plexPlayer.isCommandSpy() ? PlexUtils.messageString("enabled") : PlexUtils.messageString("disabled")).color(NamedTextColor.GRAY));
        }
        return null;
    }
}
