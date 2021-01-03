package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.command.impl.AdminCMD;
import dev.plex.command.impl.AdventureCMD;
import dev.plex.command.impl.BanCMD;
import dev.plex.command.impl.CreativeCMD;
import dev.plex.command.impl.FionnCMD;
import dev.plex.command.impl.FreezeCMD;
import dev.plex.command.impl.NameHistoryCMD;
import dev.plex.command.impl.OpAllCMD;
import dev.plex.command.impl.OpCMD;
import dev.plex.command.impl.PlexCMD;
import dev.plex.command.impl.PunishmentsCMD;
import dev.plex.command.impl.SpectatorCMD;
import dev.plex.command.impl.SurvivalCMD;
import dev.plex.command.impl.TestCMD;
import dev.plex.command.impl.WorldCMD;
import java.util.List;
import dev.plex.command.PlexCommand;
import dev.plex.util.PlexLog;

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
