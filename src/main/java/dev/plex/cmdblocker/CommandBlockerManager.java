package dev.plex.cmdblocker;

import dev.plex.Plex;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class CommandBlockerManager
{
    private List<BaseCommand> blockedCommands = new ArrayList<>();

    public boolean loadedYet = false;

    public void syncCommands()
    {
        loadedYet = false;
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
                int ind = regexOrMatch.indexOf(' ');
                if (ind == -1 && regexOrMatch.endsWith(":"))
                {
                    //block all commands from this plugin for the specified rank
                    Plugin plugin = Arrays.stream(Plex.get().getServer().getPluginManager().getPlugins()).findAny().orElse(null);
                    if (plugin != null)
                    {
                        List<Command> commandList = PluginCommandYamlParser.parse(plugin);
                        for (Command command : commandList)
                        {
                            blockedCommands.add(new MatchCommand(command.getName(), rank, message));
                        }
                    }
                }
                String blockedArgs = ind == -1 ? "" : regexOrMatch.substring(ind + 1);
                if (!blockedArgs.isEmpty()) blockedArgs = " " + blockedArgs; // necessary in case no args
                PluginCommand pluginCommand = Plex.get().getServer().getPluginCommand(ind == -1 ? regexOrMatch : regexOrMatch.substring(0, ind));
                if (pluginCommand != null)
                {
                    blockedCommands.add(new MatchCommand(pluginCommand.getName() + blockedArgs, rank, message));
                    List<String> aliases = pluginCommand.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + blockedArgs, rank, message));
                    }
                }
            }
        }

        loadedYet = true;
    }
}
