package dev.plex.util;

import dev.plex.Plex;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;

import java.util.List;
import java.util.Locale;
import org.apache.commons.lang3.StringUtils;

public final class CommandUtils
{
    private CommandUtils()
    {
    }

    public static boolean matchesCommand(Plex plugin, String commandLine, List<String> commandNames)
    {
        if (StringUtils.isBlank(commandLine))
        {
            return false;
        }

        String input = commandLine.charAt(0) == '/' ? commandLine.substring(1) : commandLine;
        String label = input.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        int namespaceSeparator = label.indexOf(':');
        String bareLabel = label.substring(namespaceSeparator + 1);
        Command enteredCommand = Bukkit.getCommandMap().getCommand(label);

        for (String commandName : commandNames)
        {
            if (commandName.equalsIgnoreCase(label))
            {
                return true;
            }
            if (commandName.equalsIgnoreCase(bareLabel))
            {
                return true;
            }
            if (plugin.getCommandHandler() == null)
            {
                continue;
            }
            if (plugin.getCommandHandler().isAliasFor(commandName, bareLabel))
            {
                return true;
            }
            Command configuredCommand = Bukkit.getCommandMap().getCommand(commandName);
            if (enteredCommand != null && enteredCommand.equals(configuredCommand))
            {
                return true;
            }
        }
        return false;
    }
}
