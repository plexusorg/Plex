package me.totalfreedom.plex.handlers;

import com.google.common.collect.Lists;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.impl.*;
import me.totalfreedom.plex.util.PlexLog;

import java.util.List;

public class CommandHandler
{

    private List<PlexCommand> commands = Lists.newArrayList();

    public CommandHandler()
    {
        commands.add(new TestCMD());
        commands.add(new PlexCMD());
        commands.add(new FionnCMD());
        commands.add(new WorldCMD());
        commands.add(new OpAllCMD());
        commands.add(new OpCMD());

        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }

}
