package dev.plex.command.impl.brigadier;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexBrigadierCommand;
import dev.plex.command.annotation.CommandName;
import dev.plex.command.annotation.CommandPermission;
import dev.plex.command.annotation.CommandSource;
import dev.plex.command.annotation.Default;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import org.apache.commons.lang3.BooleanUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dev.plex.util.PlexUtils.messageComponent;

/**
 * @author Taah
 * @project Plex
 * @since 6:54 AM [09-07-2023]
 */
@CommandName({"adminchat", "o", "sc", "ac", "staffchat"})
public class AdminChatCMD extends PlexBrigadierCommand
{
    @Default
    @CommandPermission("plex.adminchat")
    @CommandSource(RequiredCommandSource.IN_GAME)
    public void toggle(Player sender) {
        PlexPlayer player = DataUtils.getPlayer(sender.getUniqueId());
        player.setStaffChat(!player.isStaffChat());
        send(sender, messageComponent("adminChatToggled", BooleanUtils.toStringOnOff(player.isStaffChat())));
    }
}
