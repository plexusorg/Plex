package dev.plex.command;

import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import dev.plex.Plex;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class PlexCommand implements SimpleCommand
{
    /**
     * Returns the instance of the plugin
     */
    protected static Plex plugin = Plex.get();

    /**
     * The parameters for the command
     */
    private final CommandParameters params;

    /**
     * The permissions for the command
     */
    private final CommandPermissions perms;
    /**
     * Required command source fetched from the permissions
     */
    private final RequiredCommandSource commandSource;

    public PlexCommand()
    {
        this.params = getClass().getAnnotation(CommandParameters.class);
        this.perms = getClass().getAnnotation(CommandPermissions.class);
        this.commandSource = this.perms.source();

        CommandMeta.Builder meta = plugin.getServer().getCommandManager().metaBuilder(this.params.name());
        meta.aliases(this.params.aliases());
        meta.plugin(Plex.get());
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
        if (!perms.permission().isEmpty())
        {
            if (!invocation.source().hasPermission(perms.permission()))
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
        if (params.name().equalsIgnoreCase(label))
        {
            return true;
        }
        return Arrays.stream(params.aliases()).anyMatch(s -> s.equalsIgnoreCase(label));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }
}
