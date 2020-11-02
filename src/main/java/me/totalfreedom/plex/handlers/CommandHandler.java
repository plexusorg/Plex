package me.totalfreedom.plex.handlers;

import com.google.common.collect.Lists;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.impl.FionnCMD;
import me.totalfreedom.plex.command.impl.PlexCMD;
import me.totalfreedom.plex.command.impl.TestCMD;
import me.totalfreedom.plex.command.impl.WorldCMD;
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

        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }

}
