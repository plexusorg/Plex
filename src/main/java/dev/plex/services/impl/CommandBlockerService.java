package dev.plex.services.impl;

import com.google.common.collect.Lists;
import dev.plex.command.blocking.BlockedCommand;
import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.Command;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CommandBlockerService extends AbstractService
{
    @Getter
    private static final List<BlockedCommand> BLOCKED_COMMANDS = Lists.newArrayList();

    public CommandBlockerService()
    {
        super(false, false);
    }

    @Override
    public void run()
    {
        BLOCKED_COMMANDS.clear();
        PlexLog.debug("RUNNING COMMAND BLOCKING SERVICE");
        plugin.commands.getStringList("commands").forEach(s ->
        {
            BlockedCommand command = new BlockedCommand();
            int lastDelim = s.lastIndexOf(':');

            String cmdWithoutMsg = s.substring(0, lastDelim);
            String[] args = cmdWithoutMsg.split(":", 3); // Delimiter code by ayunami
            if (s.toLowerCase(Locale.ROOT).startsWith("r"))
            {
                command.setRequiredLevel(args[1]);
                command.setRegex(args[2]);
                command.setMessage(s.substring(lastDelim + 1));
                PlexLog.debug("=Found regex blocked=");
                PlexLog.debug(" Regex: " + command.getRegex());
                PlexLog.debug(" Message: " + command.getMessage());
                PlexLog.debug("====================");
            } else if (s.toLowerCase(Locale.ROOT).startsWith("m"))
            {
                command.setRequiredLevel(args[1]);
                command.setCommand(args[2]);
                command.setMessage(s.substring(lastDelim + 1));
                Command cmd = plugin.getServer().getCommandMap().getCommand(command.getCommand().split(" ")[0]);
                if (cmd == null)
                {
                    PlexLog.error("Command '{0}' specified in the configuration was null!", command.getCommand().split(" ")[0]);
                    return;
                }
                command.setCommandAliases(cmd.getAliases());
                command.getCommandAliases().add(command.getCommand().split(" ")[0]);
                PlexLog.debug("=Found command blocked=");
                PlexLog.debug(" Required Level: " + command.getRequiredLevel());
                PlexLog.debug(" Command: " + command.getCommand());
                PlexLog.debug(" Message: " + command.getMessage());
                PlexLog.debug(" Aliases: " + Arrays.toString(command.getCommandAliases().toArray(new String[0])));
                PlexLog.debug("====================");
            }
            if (command.getMessage().equalsIgnoreCase("_"))
            {
                command.setMessage("This command is blocked.");
            }
            BLOCKED_COMMANDS.add(command);
        });
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}
