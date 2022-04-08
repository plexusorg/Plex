package dev.plex.cmdblocker;

import dev.plex.Plex;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import lombok.Getter;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class CommandBlockerManager
{
    private List<BaseCommand> blockedCommands = new ArrayList<>();

    public boolean loadedYet = false;

    public void syncCommands()
    {
        blockedCommands.clear();

        List<String> raw = Plex.get().blockedCommands.getStringList("blockedCommands");

        for (String cmd : raw)
        {
            int lastDelim = cmd.lastIndexOf(':');

            String cmdWithoutMsg = cmd.substring(0, lastDelim);
            String[] rawPieces = cmdWithoutMsg.split(":", 3);

            String rawType = rawPieces[0].toLowerCase();
            String rawRank = rawPieces[1].toLowerCase();
            String regexOrMatch = rawPieces[2];
            String message = cmd.substring(lastDelim + 1);

            if (message.equals("_"))
            {
                message = PlexUtils.messageString("commandBlocked");
            }

            Rank rank;

            switch (rawRank)
            {
                case "i":
                    rank = Rank.IMPOSTOR;
                    break;
                case "n":
                    rank = Rank.NONOP;
                    break;
                case "o":
                    rank = Rank.OP;
                    break;
                case "a":
                    rank = Rank.ADMIN;
                    break;
                case "s":
                    rank = Rank.SENIOR_ADMIN;
                    break;
                case "e":
                    rank = Rank.EXECUTIVE;
                    break;
                default:
                    rank = Rank.EXECUTIVE;
            }

            if (rawType.equals("r"))
            {
                blockedCommands.add(new RegexCommand(Pattern.compile(regexOrMatch, Pattern.CASE_INSENSITIVE), rank, message));
            }
            else if (rawType.equals("m"))
            {
                String blockedArgs = regexOrMatch.substring(regexOrMatch.indexOf(' ') + 1);
                PluginCommand pluginCommand = Plex.get().getServer().getPluginCommand(regexOrMatch.substring(0, regexOrMatch.indexOf(' ')));
                if (pluginCommand != null)
                {
                    blockedCommands.add(new MatchCommand(pluginCommand.getName() + " " + blockedArgs, rank, message));
                    List<String> aliases = pluginCommand.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + " " + blockedArgs, rank, message));
                    }
                }
            }
        }

        loadedYet = true;
    }
}
