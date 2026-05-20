package dev.plex.command;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.plex.Plex;
import dev.plex.command.source.RequiredCommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class ProxyCommand implements SimpleCommand
{
    protected final Plex plugin;
    private final CommandSpec commandSpec;
    private final RequiredCommandSource commandSource;

    /**
     * Creates and registers a proxy command using the current proxy plugin.
     *
     * @param commandSpec explicit command metadata
     */
    protected ProxyCommand(CommandSpec commandSpec)
    {
        this(Plex.get(), commandSpec);
    }

    /**
     * Creates and registers a proxy command.
     *
     * @param plugin running proxy plugin
     * @param commandSpec explicit command metadata
     */
    protected ProxyCommand(Plex plugin, CommandSpec commandSpec)
    {
        this.plugin = plugin;
        this.commandSpec = commandSpec;
        this.commandSource = commandSpec.requiredSource();

        CommandMeta.Builder meta = plugin.getServer().getCommandManager().metaBuilder(commandSpec.name());
        if (!commandSpec.aliases().isEmpty())
        {
            meta.aliases(commandSpec.aliases().toArray(String[]::new));
        }
        meta.plugin(plugin);
        plugin.getServer().getCommandManager().register(meta.build(), this);
    }

    protected abstract Component execute(@NotNull CommandSource source, @Nullable Player player, @NotNull String[] args);

    @Override
    public void execute(Invocation invocation)
    {
        if (!matches(invocation.alias()))
        {
            return;
        }

        if (commandSource == RequiredCommandSource.CONSOLE && invocation.source() instanceof Player)
        {
            //            sender.sendMessage(messageComponent("noPermissionInGame"));
            return;
        }

        if (commandSource == RequiredCommandSource.IN_GAME)
        {
            if (invocation.source() instanceof ConsoleCommandSource)
            {
                //                send(sender, messageComponent("noPermissionConsole"));
                return;
            }
        }
        if (!commandSpec.permission().isEmpty())
        {
            if (!invocation.source().hasPermission(commandSpec.permission()))
            {
                return;
            }
        }
        Component component = this.execute(invocation.source(), invocation.source() instanceof ConsoleCommandSource ? null : (Player) invocation.source(), invocation.arguments());
        if (component != null)
        {
            send(invocation.source(), component);
        }
    }

    private boolean matches(String label)
    {
        if (commandSpec.name().equalsIgnoreCase(label))
        {
            return true;
        }
        return commandSpec.aliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(label));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }
}
