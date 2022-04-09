package dev.plex.command.blocker;

import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class CommandBlockerManager extends PlexBase
{
    private List<BaseCommand> blockedCommands = new ArrayList<>();

    public boolean loadedYet;

    public void syncCommands()
    {
        loadedYet = false;
        blockedCommands.clear();

        List<String> raw = plugin.blockedCommands.getStringList("blockedCommands");

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

            Rank rank = switch (rawRank)
                    {
                        case "e" -> null;
                        case "a" -> Rank.ADMIN;
                        case "s" -> Rank.SENIOR_ADMIN;
                        default -> null;
                    };

            if (rawType.equals("r"))
            {
                blockedCommands.add(new RegexCommand(Pattern.compile(regexOrMatch, Pattern.CASE_INSENSITIVE), rank, message));
            }
            else if (rawType.equals("m"))
            {
                int ind = regexOrMatch.indexOf(' ');
                if (ind == -1 && regexOrMatch.endsWith(":"))
                {
                    String pluginName = regexOrMatch.substring(0, regexOrMatch.length() - 1);
                    Plugin plugin = Arrays.stream(Bukkit.getServer().getPluginManager().getPlugins()).filter(pl -> pl.getName().equalsIgnoreCase(pluginName)).findAny().orElse(null);
                    if (plugin != null)
                    {
                        List<Command> commandList = PluginCommandYamlParser.parse(plugin);
                        for (Command command : commandList)
                        {
                            blockedCommands.add(new MatchCommand(command.getName(), rank, message));
                            blockedCommands.add(new MatchCommand(pluginName + ":" + command.getName(), rank, message));
                            for (String alias : command.getAliases())
                            {
                                blockedCommands.add(new MatchCommand(alias, rank, message));
                                blockedCommands.add(new MatchCommand(pluginName + ":" + alias, rank, message));
                            }
                        }
                    }
                }
                String blockedArgs = ind == -1 ? "" : regexOrMatch.substring(ind + 1);
                if (!blockedArgs.isEmpty())
                {
                    blockedArgs = " " + blockedArgs; // necessary in case no args
                }
                PluginCommand pluginCommand = Plex.get().getServer().getPluginCommand(ind == -1 ? regexOrMatch : regexOrMatch.substring(0, ind));
                if (pluginCommand != null)
                {
                    String pluginName = pluginCommand.getPlugin().getName();
                    blockedCommands.add(new MatchCommand(pluginCommand.getName() + blockedArgs, rank, message));
                    blockedCommands.add(new MatchCommand(pluginName + ":" + pluginCommand.getName() + blockedArgs, rank, message));
                    List<String> aliases = pluginCommand.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + blockedArgs, rank, message));
                        blockedCommands.add(new MatchCommand(pluginName + ":" + alias + blockedArgs, rank, message));
                    }
                }
            }
        }

        loadedYet = true;
    }
}
