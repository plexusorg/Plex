package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.impl.*;
import dev.plex.util.PlexLog;

import java.util.List;

public class CommandHandler
{
    public CommandHandler()
    {
        List<PlexCommand> commands = Lists.newArrayList();
        commands.add(new AdminCMD());
        commands.add(new AdminworldCMD());
        commands.add(new AdventureCMD());
        commands.add(new BanCMD());
        commands.add(new CreativeCMD());
        commands.add(new FlatlandsCMD());
        commands.add(new FreezeCMD());
        commands.add(new ListCMD());
        commands.add(new MasterbuilderworldCMD());
        commands.add(new NameHistoryCMD());
        commands.add(new OpAllCMD());
        commands.add(new OpCMD());
        commands.add(new PlexCMD());
        commands.add(new PunishmentsCMD());
        commands.add(new RankCMD());
        commands.add(new SpectatorCMD());
        commands.add(new SurvivalCMD());
        commands.add(new WorldCMD());
        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }
}
