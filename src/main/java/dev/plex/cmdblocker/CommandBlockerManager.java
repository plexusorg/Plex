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
            List<String> pieces = new ArrayList<>();

            int lastDelim = cmd.lastIndexOf(':');

            String cmdWithoutMsg = cmd.substring(0, lastDelim);
            String[] rawPieces = cmdWithoutMsg.split(":", 3);

            pieces.add(rawPieces[0].toLowerCase()); // type
            pieces.add(rawPieces[1].toLowerCase()); // rank
            pieces.add(rawPieces[2]); // RegEx or match
            pieces.add(cmd.substring(lastDelim + 1)); // Message (w/o : in it)

            if (pieces.get(3).equals("_"))
            {
                pieces.set(3, PlexUtils.messageString("commandBlocked"));
            }

            Rank rank;

            switch (pieces.get(1))
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

            if (pieces.get(0).equals("r"))
            {
                blockedCommands.add(new RegexCommand(Pattern.compile(pieces.get(2), Pattern.CASE_INSENSITIVE), rank, pieces.get(3)));
            }
            else if (pieces.get(0).equals("m"))
            {
                String blockedArgs = pieces.get(2).substring(pieces.get(2).indexOf(' ') + 1);
                PluginCommand pluginCommand = Plex.get().getServer().getPluginCommand(pieces.get(2).substring(0, pieces.get(2).indexOf(' ')));
                if (pluginCommand != null)
                {
                    blockedCommands.add(new MatchCommand(pluginCommand.getName() + " " + blockedArgs, rank, pieces.get(3)));
                    List<String> aliases = pluginCommand.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + " " + blockedArgs, rank, pieces.get(3)));
                    }
                }
            }
        }

        loadedYet = true;
    }
}
