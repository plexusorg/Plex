package dev.plex.handlers;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.impl.*;
import dev.plex.util.PlexLog;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class CommandHandler
{
    private final List<PlexCommand> commands = new ArrayList<>();
    private final Map<PlexCommand, Map<String, CommandNode<CommandSourceStack>>> registeredNodes = new IdentityHashMap<>();
    private RootCommandNode<CommandSourceStack> dispatcherRoot;
    private boolean lifecycleRegistered;
    private boolean lifecycleReloadRequired;

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
        }
    }

    public void unregisterCommand(PlexCommand command)
    {
        boolean removed = commands.remove(command);
        if (removed && lifecycleRegistered)
        {
            lifecycleReloadRequired = true;
            Map<String, CommandNode<CommandSourceStack>> nodes = registeredNodes.remove(command);
            if (nodes != null)
            {
                for (var entry : nodes.entrySet())
                {
                    if (dispatcherRoot.getChild(entry.getKey()) == entry.getValue())
                    {
                        // Paper forwards command-map removal to its live Brigadier dispatcher.
                        Bukkit.getCommandMap().getKnownCommands().remove(entry.getKey());
                    }
                }
            }
        }
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
        dispatcherRoot = registrar.getDispatcher().getRoot();
        registeredNodes.clear();
        int labels = 0;
        for (PlexCommand command : commands)
        {
            Map<String, CommandNode<CommandSourceStack>> nodes = new HashMap<>();
            registeredNodes.put(command, nodes);
            LiteralCommandNode<CommandSourceStack> commandNode = command.buildCommand();
            var registeredLabels = registrar.register(commandNode, command.getDescription(), List.of());
            for (String label : registeredLabels)
            {
                nodes.put(label, dispatcherRoot.getChild(label));
            }
            labels += registeredLabels.size();

            for (String alias : command.getAliases())
            {
                // Paper does not let aliases replace an existing command. Register each
                // Plex alias as a primary command node so Plex deliberately takes priority.
                var registeredAliasLabels = registrar.register(copyWithLabel(commandNode, alias), command.getDescription(), List.of());
                for (String label : registeredAliasLabels)
                {
                    nodes.put(label, dispatcherRoot.getChild(label));
                }
                labels += registeredAliasLabels.size();
                if (!registeredAliasLabels.contains(alias) && !registeredAliasLabels.contains("plex:" + alias))
                {
                    PlexLog.warn("Command alias {0} for {1} was not registered, likely because another command already owns it.", alias, command.getName());
                }
            }
        }
        lifecycleRegistered = true;
        lifecycleReloadRequired = false;
        PlexLog.log("Registered {0} Brigadier commands with {1} root labels.", commands.size(), labels);
    }

    private LiteralCommandNode<CommandSourceStack> copyWithLabel(LiteralCommandNode<CommandSourceStack> commandNode, String label)
    {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(label)
                .requires(commandNode.getRequirement());
        if (commandNode.getCommand() != null)
        {
            builder.executes(commandNode.getCommand());
        }
        if (commandNode.getRedirect() != null)
        {
            builder.forward(commandNode.getRedirect(), commandNode.getRedirectModifier(), commandNode.isFork());
        }
        else
        {
            commandNode.getChildren().forEach(builder::then);
        }
        return builder.build();
    }

    private void registerBuiltInCommands(boolean debugEnabled)
    {
        commands.addAll(List.of(
                new AdminChatCMD(),
                new AdminworldCMD(),
                new BanCMD(),
                new BanIpCMD(),
                new BanListCommand(),
                new BanNameCMD(),
                new BlockEditCMD(),
                new CommandSpyCMD(),
                new ConsoleSayCMD(),
                new EntityWipeCMD(),
                new FlatlandsCMD(),
                new FreezeCMD(),
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
        if (Plex.get().getWorldGuardHook() != null)
        {
            commands.add(new ProtectCMD(Plex.get().getWorldGuardHook()));
        }
    }
}
