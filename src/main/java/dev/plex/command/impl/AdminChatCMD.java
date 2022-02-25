package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.listener.impl.ChatListener;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.adminchat", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adminchat",  description = "Talk privately with other admins", usage = "/<command> <message>", aliases = "o,ac,sc,staffchat")
public class AdminChatCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        ChatListener.adminChat(sender, StringUtils.join(args, " "));
        return null;
    }
}
