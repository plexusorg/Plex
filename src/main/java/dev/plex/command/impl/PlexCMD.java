package dev.plex.command.impl;

import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import org.bukkit.ChatColor;

import java.util.Arrays;
import java.util.List;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(usage = "/<command> [reload]", aliases = "plexhelp", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand {
    public PlexCMD() {
        super("plex");
    }

    @Override
    public void execute(CommandSource sender, String[] args) {
        if (args.length == 0) {
            send(ChatColor.LIGHT_PURPLE + "Plex. The long awaited TotalFreedomMod rewrite starts here...");
            send(ChatColor.LIGHT_PURPLE + "Plugin version: " + ChatColor.GOLD + "1.0");
            return;
        }
        if (args[0].equals("reload"))
        {
            if (!plugin.getRankManager().isSeniorAdmin(sender.getPlexPlayer()))
            {
                send(tl("noPermission"));
                return;
            }
            Plex.get().config.load();
            send("Reloaded config file");
            Plex.get().messages.load();
            send("Reloaded messages file");
            Plex.get().getRankManager().importDefaultRanks();
            send("Imported ranks");
            send("Plex successfully reloaded.");
        } else {
            throw new CommandArgumentException();
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return List.of("reload");
    }
}