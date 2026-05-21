package dev.plex.handlers;

import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.impl.*;
import dev.plex.util.PlexLog;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public class CommandHandler
{
    private final List<PlexCommand> commands = new ArrayList<>();
    private boolean lifecycleRegistered;
    private boolean lifecycleReloadRequired;
    private boolean suppressLifecycleWarnings;

    public CommandHandler(Plex plugin)
    {
        registerBuiltInCommands(plugin.config.getBoolean("debug"));
        commands.addAll(plugin.getPendingCommands());
        plugin.getPendingCommands().clear();
        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> register(event.registrar()));
    }

    public void registerCommand(PlexCommand command)
    {
        commands.add(command);
        if (lifecycleRegistered)
        {
            lifecycleReloadRequired = true;
            if (!suppressLifecycleWarnings)
            {
                PlexLog.warn("Command {0} was registered after the Brigadier command lifecycle event; it will be included on the next command lifecycle rebuild.", command.getName());
            }
        }
    }

    public void unregisterCommand(PlexCommand command)
    {
        boolean removed = commands.remove(command);
        if (removed && lifecycleRegistered)
        {
            lifecycleReloadRequired = true;
            if (!suppressLifecycleWarnings)
            {
                PlexLog.warn("Command {0} was unregistered after the Brigadier command lifecycle event; Paper may keep the active Brigadier node until the next command lifecycle rebuild.", command.getName());
            }
        }
    }

    public boolean setSuppressLifecycleWarnings(boolean suppress)
    {
        boolean previous = suppressLifecycleWarnings;
        suppressLifecycleWarnings = suppress;
        return previous;
    }

    public boolean requiresLifecycleReload()
    {
        return lifecycleReloadRequired;
    }

    public List<PlexCommand> getCommands()
    {
        return List.copyOf(commands);
    }

    public @Nullable PlexCommand getCommand(String name)
    {
        String normalized = name.toLowerCase(Locale.ROOT);
        return commands.stream()
                .filter(command -> command.getName().equalsIgnoreCase(name) ||
                        command.getAliases().stream().map(alias -> alias.toLowerCase(Locale.ROOT)).toList().contains(normalized))
                .findFirst()
                .orElse(null);
    }

    public boolean isAliasFor(String commandName, String alias)
    {
        PlexCommand command = getCommand(commandName);
        if (command == null)
        {
            return false;
        }
        String normalized = alias.toLowerCase(Locale.ROOT);
        return command.getAliases().stream().map(value -> value.toLowerCase(Locale.ROOT)).toList().contains(normalized);
    }

    private void register(Commands registrar)
    {
        int labels = 0;
        for (PlexCommand command : commands)
        {
            var registeredLabels = registrar.register(command.buildCommand(), command.getDescription(), command.getAliases());
            labels += registeredLabels.size();
            for (String alias : command.getAliases())
            {
                if (!registeredLabels.contains(alias) && !registeredLabels.contains("plex:" + alias))
                {
                    PlexLog.warn("Command alias {0} for {1} was not registered, likely because another command already owns it.", alias, command.getName());
                }
            }
        }
        lifecycleRegistered = true;
        lifecycleReloadRequired = false;
        PlexLog.log("Registered {0} Brigadier commands with {1} root labels.", commands.size(), labels);
    }

    private void registerBuiltInCommands(boolean debugEnabled)
    {
        commands.addAll(List.of(
                new AdminChatCMD(),
                new AdminworldCMD(),
                new AdventureCMD(),
                new BanCMD(),
                new BanListCommand(),
                new BcastLoginMessageCMD(),
                new BlockEditCMD(),
                new CommandSpyCMD(),
                new ConsoleSayCMD(),
                new CreativeCMD(),
                new EntityWipeCMD(),
                new FlatlandsCMD(),
                new FreezeCMD(),
                new GamemodeCMD(),
                new KickCMD(),
                new ListCMD(),
                new LocalSpawnCMD(),
                new LockupCMD(),
                new MasterbuilderworldCMD(),
                new MobLimitCMD(),
                new MobPurgeCMD(),
                new MuteCMD(),
                new NotesCMD(),
                new PlexCMD(),
                new PunishmentsCMD(),
                new RawSayCMD(),
                new RemoveLoginMessageCMD(),
                new SayCMD(),
                new SetLoginMessageCMD(),
                new SmiteCMD(),
                new SpectatorCMD(),
                new SurvivalCMD(),
                new TagCMD(),
                new TempbanCMD(),
                new TempmuteCMD(),
                new ToggleCMD(),
                new UnbanCMD(),
                new UnfreezeCMD(),
                new UnmuteCMD(),
                new WhoHasCMD(),
                new WorldCMD()
        ));
        if (debugEnabled)
        {
            commands.add(new DebugCMD());
        }
    }
}
