package dev.plex.command.blocker;

import dev.plex.PlexBase;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import java.util.*;
import java.util.regex.Pattern;

@Getter
public class CommandBlockerManager extends PlexBase
{
    private Set<BaseCommand> blockedCommands = new HashSet<>();

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
                String cmdForSearch = ind == -1 ? regexOrMatch : regexOrMatch.substring(0, ind);
                PluginCommand pluginCommand = Bukkit.getServer().getPluginCommand(cmdForSearch);
                Plugin pl = null;
                if (pluginCommand != null) pl = pluginCommand.getPlugin();
                Command command = plugin.getServer().getCommandMap().getCommand(cmdForSearch);
                if (command != null)
                {
                    String pluginName = pl == null ? null : pl.getName();
                    blockedCommands.add(new MatchCommand(command.getName() + blockedArgs, rank, message));
                    if (pluginName != null) blockedCommands.add(new MatchCommand(pluginName + ":" + command.getName() + blockedArgs, rank, message));
                    List<String> aliases = command.getAliases();
                    for (String alias : aliases)
                    {
                        blockedCommands.add(new MatchCommand(alias + blockedArgs, rank, message));
                        if (pluginName != null) blockedCommands.add(new MatchCommand(pluginName + ":" + alias + blockedArgs, rank, message));
                    }
                }
                else
                {
                    // fallback to basic blocking
                    blockedCommands.add(new MatchCommand(cmdForSearch + blockedArgs, rank, message));
                }
            }
        }

        loadedYet = true;
    }
}
