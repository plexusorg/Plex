package me.totalfreedom.plex.handlers;

import com.google.common.collect.Lists;
import java.util.List;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.impl.AdminCMD;
import me.totalfreedom.plex.command.impl.FionnCMD;
import me.totalfreedom.plex.command.impl.FreezeCMD;
import me.totalfreedom.plex.command.impl.NameHistoryCMD;
import me.totalfreedom.plex.command.impl.OpAllCMD;
import me.totalfreedom.plex.command.impl.OpCMD;
import me.totalfreedom.plex.command.impl.PlexCMD;
import me.totalfreedom.plex.command.impl.TestCMD;
import me.totalfreedom.plex.command.impl.WorldCMD;
import me.totalfreedom.plex.util.PlexLog;

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
        commands.add(new FreezeCMD());
        commands.add(new NameHistoryCMD());
        commands.add(new AdminCMD());

        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }

}
