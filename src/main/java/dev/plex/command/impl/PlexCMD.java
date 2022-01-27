package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(name = "plex", usage = "/<command> [reload]", aliases = "plexhelp", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand {

    @Override
    public Component execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            send(sender, ChatColor.LIGHT_PURPLE + "Plex. The long awaited TotalFreedomMod rewrite starts here...");
            return componentFromString(ChatColor.LIGHT_PURPLE + "Plugin version: " + ChatColor.GOLD + "1.0");
        }
        if (args[0].equals("reload"))
        {
            if (!isSeniorAdmin(sender))
            {
                return tl("noPermission");
            }
            Plex.get().config.load();
            send(sender, "Reloaded config file");
            Plex.get().messages.load();
            send(sender, "Reloaded messages file");
            Plex.get().getRankManager().importDefaultRanks();
            send(sender, "Imported ranks");
            send(sender, "Plex successfully reloaded.");
        } else {
            throw new CommandArgumentException();
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return ImmutableList.of("reload");
    }
    
}