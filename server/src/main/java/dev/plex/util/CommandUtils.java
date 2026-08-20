package dev.plex.util;

import dev.plex.Plex;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;

import java.util.List;
import java.util.Locale;

public final class CommandUtils
{
    private CommandUtils()
    {
    }

    public static boolean matchesCommand(Plex plugin, String commandLine, List<String> commandNames)
    {
        if (commandLine == null || commandLine.isBlank())
        {
            return false;
        }

        String input = commandLine.charAt(0) == '/' ? commandLine.substring(1) : commandLine;
        if (input.isBlank())
        {
            return false;
        }

        String label = input.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        String bareLabel = label.contains(":") ? label.substring(label.indexOf(':') + 1) : label;
        Command enteredCommand = Bukkit.getCommandMap().getCommand(label);

        for (String commandName : commandNames)
        {
            if (commandName.equalsIgnoreCase(label) || commandName.equalsIgnoreCase(bareLabel))
            {
                return true;
            }
            if (plugin.getCommandHandler() != null && plugin.getCommandHandler().isAliasFor(commandName, bareLabel))
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
