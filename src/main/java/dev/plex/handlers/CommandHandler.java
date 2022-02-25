package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.command.PlexCommand;
import dev.plex.command.impl.*;
import dev.plex.util.PlexLog;
import java.util.List;

//TODO: Switch to Reflections API
public class CommandHandler extends PlexBase
{
    public CommandHandler()
    {
        List<PlexCommand> commands = Lists.newArrayList();
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            commands.add(new AdminCMD());
            commands.add(new DeopAllCMD());
            commands.add(new DeopCMD());
            commands.add(new OpAllCMD());
            commands.add(new OpCMD());
            commands.add(new RankCMD());
        }
        if (plugin.config.getBoolean("debug"))
        {
            commands.add(new DebugCMD());
        }

        commands.add(new AdminChatCMD());
        commands.add(new AdminworldCMD());
        commands.add(new AdventureCMD());
        commands.add(new BanCMD());
        commands.add(new CommandSpyCMD());
        commands.add(new CreativeCMD());
        commands.add(new FlatlandsCMD());
        commands.add(new FreezeCMD());
        commands.add(new ListCMD());
        commands.add(new LocalSpawnCMD());
        commands.add(new MasterbuilderworldCMD());
        commands.add(new MuteCMD());
        commands.add(new NameHistoryCMD());
        commands.add(new PlexCMD());
        commands.add(new PunishmentsCMD());
        commands.add(new RawSayCMD());
        commands.add(new SpectatorCMD());
        commands.add(new SurvivalCMD());
        commands.add(new TagCMD());
        commands.add(new UnbanCMD());
        commands.add(new UnfreezeCMD());
        commands.add(new UnmuteCMD());
        commands.add(new WorldCMD());
        PlexLog.log(String.format("Registered %s commands!", commands.size()));
    }
}
