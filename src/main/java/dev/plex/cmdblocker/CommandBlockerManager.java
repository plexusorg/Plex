package dev.plex.cmdblocker;

import dev.plex.Plex;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.PluginCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
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

            if (pieces.get(3).equals("_"))
            {
                pieces.set(3, PlexUtils.messageString("commandBlocked"));
            }

            Rank rank = Plex.get().getRankManager().getRankFromString(pieces.get(1));

            if (pieces.get(0).equals("r"))
            {
                blockedCommands.add(new RegexCommand(Pattern.compile(pieces.get(2), Pattern.CASE_INSENSITIVE), rank, pieces.get(3)));
            }
            else if (pieces.get(0).equals("m"))
            {
                blockedCommands.add(new MatchCommand(pieces.get(2), rank, pieces.get(3)));
                String blockedArgs = pieces.get(2).substring(pieces.get(2).indexOf(' ') + 1);
                PluginCommand pluginCommand = Plex.get().getServer().getPluginCommand(pieces.get(2).substring(0, pieces.get(2).indexOf(' ')));
                if (pluginCommand != null)
                {
                    List<String> aliases = pluginCommand.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + " " + blockedArgs, rank, pieces.get(3)));
                    }
                }
            }
        }
    }
}
