package dev.plex.cmdblocker;

import dev.plex.Plex;
import dev.plex.rank.enums.Rank;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommandBlockerManager
{
    private List<BaseCommand> blockedCommands = new ArrayList<>();

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

            Rank rank = Plex.get().getRankManager().getRankFromString(pieces.get(1));

            if (pieces.get(0).equals("r"))
            {
                blockedCommands.add(new RegexCommand(Pattern.compile(pieces.get(2)), rank, pieces.get(3)));
            }
            else if (pieces.get(0).equals("m"))
            {
                blockedCommands.add(new MatchCommand(pieces.get(2), rank, pieces.get(3)));
            }
        }
    }
}
