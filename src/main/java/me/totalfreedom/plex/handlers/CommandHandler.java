package me.totalfreedom.plex.handlers;

import com.google.common.collect.Lists;
import java.util.List;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.impl.*;
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
        commands.add(new AdventureCMD());
        commands.add(new CreativeCMD());
        commands.add(new SurvivalCMD());
        commands.add(new SpectatorCMD());
        commands.add(new BanCMD());
        commands.add(new PunishmentsCMD());
        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }
}
