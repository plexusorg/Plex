package dev.plex.command;

import java.util.ArrayList;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Public base class for module commands. */
public abstract class PlexCommand extends Command
{
    private static Runtime runtime;
    private final CommandParameters params;
    private final CommandPermissions perms;
    private final RequiredCommandSource commandSource;

    public static void setRuntime(Runtime runtime)
    {
        PlexCommand.runtime = runtime;
    }

    public PlexCommand(boolean register)
    {
        super("");
        this.params = getClass().getAnnotation(CommandParameters.class);
        this.perms = getClass().getAnnotation(CommandPermissions.class);
        if (params == null || perms == null)
        {
            throw new IllegalStateException("PlexCommand requires CommandParameters and CommandPermissions annotations");
        }
        setName(params.name());
        setLabel(params.name());
        setDescription(params.description());
        setPermission(perms.permission());
        setUsage(params.usage().replace("<command>", params.name()));
        if (!params.aliases().isEmpty())
        {
            setAliases(Arrays.asList(params.aliases().split(",")));
        }
        this.commandSource = perms.source();
        if (register)
        {
            requireRuntime().register(this);
        }
    }

    public PlexCommand()
    {
        this(true);
    }

    protected abstract Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args);

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args)
    {
        if (!matches(label))
        {
            return false;
        }
        if (commandSource == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            send(sender, messageComponent("noPermissionInGame"));
            return true;
        }
        if (commandSource == RequiredCommandSource.IN_GAME && sender instanceof ConsoleCommandSender)
        {
            send(sender, messageComponent("noPermissionConsole"));
            return true;
        }
        if (!perms.permission().isEmpty() && sender instanceof Player player && !player.hasPermission(perms.permission()))
        {
            send(sender, messageComponent("noPermissionNode", perms.permission()));
            return true;
        }
        try
        {
            Component component = execute(sender, isConsole(sender) ? null : (Player)sender, args);
            if (component != null)
            {
                send(sender, component);
            }
        }
        catch (PlayerNotFoundException | CommandFailException | ConsoleOnlyException |
               ConsoleMustDefinePlayerException | PlayerNotBannedException | NumberFormatException ex)
        {
            send(sender, mmString(ex.getMessage()));
        }
        return true;
    }

    @NotNull
    public abstract List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException;

    @NotNull
    @Override
    public List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return StringUtil.copyPartialMatches(args[args.length - 1], smartTabComplete(sender, alias, args), new ArrayList<>());
    }

    private boolean matches(String label)
    {
        return getName().equalsIgnoreCase(label) || getAliases().stream().anyMatch(alias -> alias.equalsIgnoreCase(label));
    }

    protected void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    protected boolean checkPermission(CommandSender sender, String permission)
    {
        return isConsole(sender) || checkPermission((Player)sender, permission);
    }

    protected boolean silentCheckPermission(CommandSender sender, String permission)
    {
        return isConsole(sender) || silentCheckPermission((Player)sender, permission);
    }

    protected boolean checkPermission(Player player, String permission)
    {
        if (!permission.isEmpty() && !player.hasPermission(permission))
        {
            throw new CommandFailException(messageString("noPermissionNode", permission));
        }
        return true;
    }

    protected boolean silentCheckPermission(Player player, String permission)
    {
        return permission.isEmpty() || player.hasPermission(permission);
    }

    protected UUID getUUID(CommandSender sender)
    {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    protected Component messageComponent(String s, Object... objects)
    {
        return requireRuntime().messageComponent(s, objects);
    }

    protected Component messageComponent(String s, Component... objects)
    {
        return requireRuntime().messageComponent(s, objects);
    }

    @Override
    public Component permissionMessage()
    {
        return messageComponent("noPermissionNode", getPermission());
    }

    protected String messageString(String s, Object... objects)
    {
        return requireRuntime().messageString(s, objects);
    }

    protected Component usage()
    {
        return Component.text("Correct Usage: ").color(NamedTextColor.YELLOW).append(componentFromString(getUsage()).color(NamedTextColor.GRAY));
    }

    protected Component usage(String s)
    {
        return Component.text("Correct Usage: ").color(NamedTextColor.YELLOW).append(componentFromString(s).color(NamedTextColor.GRAY));
    }

    protected Player getNonNullPlayer(String name)
    {
        Player player;
        try
        {
            player = Bukkit.getPlayer(UUID.fromString(name));
        }
        catch (IllegalArgumentException ignored)
        {
            player = Bukkit.getPlayer(name);
        }
        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        return player;
    }

    protected World getNonNullWorld(String name)
    {
        World world = Bukkit.getWorld(name);
        if (world == null)
        {
            throw new CommandFailException(messageString("worldNotFound"));
        }
        return world;
    }

    protected Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    protected Component noColorComponentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    protected Component mmString(String s)
    {
        return requireRuntime().miniMessage(s);
    }

    protected void broadcast(String miniMessage)
    {
        requireRuntime().broadcast(miniMessage);
    }

    protected void broadcast(Component component)
    {
        requireRuntime().broadcast(component);
    }

    protected List<String> onlinePlayerNames()
    {
        return requireRuntime().onlinePlayerNames();
    }

    public CommandMap getMap()
    {
        return Bukkit.getCommandMap();
    }

    private static Runtime requireRuntime()
    {
        if (runtime == null)
        {
            throw new IllegalStateException("PlexCommand runtime has not been installed by Plex");
        }
        return runtime;
    }

    public interface Runtime
    {
        void register(Command command);
        Component messageComponent(String entry, Object... objects);
        Component messageComponent(String entry, Component... objects);
        String messageString(String entry, Object... objects);
        Component miniMessage(String input);
        void broadcast(String miniMessage);
        void broadcast(Component component);
        List<String> onlinePlayerNames();
    }
}
