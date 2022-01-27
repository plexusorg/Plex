package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.command.PlexCommand;
import dev.plex.command.impl.AdminCMD;
import dev.plex.command.impl.AdminworldCMD;
import dev.plex.command.impl.AdventureCMD;
import dev.plex.command.impl.BanCMD;
import dev.plex.command.impl.CreativeCMD;
import dev.plex.command.impl.FlatlandsCMD;
import dev.plex.command.impl.FreezeCMD;
import dev.plex.command.impl.ListCMD;
import dev.plex.command.impl.MasterbuilderworldCMD;
import dev.plex.command.impl.NameHistoryCMD;
import dev.plex.command.impl.OpAllCMD;
import dev.plex.command.impl.OpCMD;
import dev.plex.command.impl.PlexCMD;
import dev.plex.command.impl.PunishmentsCMD;
import dev.plex.command.impl.RankCMD;
import dev.plex.command.impl.SpectatorCMD;
import dev.plex.command.impl.SurvivalCMD;
import dev.plex.command.impl.WorldCMD;
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
