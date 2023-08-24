package dev.plex.command;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.*;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.UUID;

/**
 * Superclass for all commands
 */
public abstract class PlexCommand extends Command implements PluginIdentifiableCommand
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
     * Minimum required rank fetched from the permissions
     */
    private final Rank level;

    /**
     * Required command source fetched from the permissions
     */
    private final RequiredCommandSource commandSource;

    /**
     * Creates an instance of the command
     */
    public PlexCommand(boolean register)
    {
        super("");
        this.params = getClass().getAnnotation(CommandParameters.class);
        this.perms = getClass().getAnnotation(CommandPermissions.class);

        setName(this.params.name());
        setLabel(this.params.name());
        setDescription(params.description());
        setUsage(params.usage().replace("<command>", this.params.name()));
        if (params.aliases().split(",").length > 0)
        {
            setAliases(Arrays.asList(params.aliases().split(",")));
        }
        this.level = perms.level();
        this.commandSource = perms.source();

        if (register)
        {
            if (getMap().getKnownCommands().containsKey(this.getName().toLowerCase()))
            {
                getMap().getKnownCommands().remove(this.getName().toLowerCase());
            }
            this.getAliases().forEach(s -> {
                if (getMap().getKnownCommands().containsKey(s.toLowerCase()))
                {
                    getMap().getKnownCommands().remove(s.toLowerCase());
                }
            });
            getMap().register("plex", this);
        }
    }

    public PlexCommand()
    {
        this(true);
    }

    /**
     * Executes the command
     *
     * @param sender       The sender of the command
     * @param playerSender The player who executed the command (null if CommandSource is console or if CommandSource is any but console executed)
     * @param args         A Kyori Component to send to the sender (can be null)
     */
    protected abstract Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args);

    /**
     * @hidden
     */
    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args)
    {
        if (!matches(label))
        {
            return false;
        }

        if (commandSource == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            sender.sendMessage(messageComponent("noPermissionInGame"));
            return true;
        }

        if (commandSource == RequiredCommandSource.IN_GAME)
        {
            if (sender instanceof ConsoleCommandSender)
            {
                send(sender, messageComponent("noPermissionConsole"));
                return true;
            }
        }

        if (sender instanceof Player player)
        {
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(player.getUniqueId());

            if (plexPlayer == null)
            {
                return false;
            }

            if (plugin.getSystem().equalsIgnoreCase("ranks"))
            {
                if (!plexPlayer.getRankFromString().isAtLeast(getLevel()))
                {
                    send(sender, messageComponent("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMessage())));
                    return true;
                }
                else
                {
                    if (getLevel().isAtLeast(Rank.ADMIN) && !plexPlayer.isAdminActive())
                    {
                        send(sender, messageComponent("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMessage())));
                        return true;
                    }
                }
            }
            else if (plugin.getSystem().equalsIgnoreCase("permissions"))
            {
                if (!perms.permission().isEmpty() && !player.hasPermission(perms.permission()))
                {
                    send(sender, messageComponent("noPermissionNode", perms.permission()));
                    return true;
                }
            }
            else
            {
                PlexLog.error("Neither permissions or ranks were selected to be used in the configuration file!");
                send(sender, "There is a server misconfiguration. Please alert a developer or the owner");
                return true;
            }
        }

        if (sender instanceof ConsoleCommandSender && !sender.getName().equalsIgnoreCase("console")) //telnet
        {
            PlexPlayer plexPlayer = DataUtils.getPlayer(sender.getName());

            if (plugin.getSystem().equalsIgnoreCase("ranks"))
            {
                if (!plexPlayer.getRankFromString().isAtLeast(getLevel()))
                {
                    send(sender, messageComponent("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMessage())));
                    return true;
                }
                else
                {
                    if (getLevel().isAtLeast(Rank.ADMIN) && !plexPlayer.isAdminActive())
                    {
                        send(sender, messageComponent("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMessage())));
                        return true;
                    }
                }
            }
            else if (plugin.getSystem().equalsIgnoreCase("permissions"))
            {
                if (!perms.permission().isEmpty() && !plugin.getPermissions().playerHas(null, Bukkit.getPlayer(plexPlayer.getName()), perms.permission()))
                {
                    send(sender, messageComponent("noPermissionNode", perms.permission()));
                    return true;
                }
            }
            else
            {
                PlexLog.error("Neither permissions or ranks were selected to be used in the configuration file!");
                send(sender, "There is a server misconfiguration. Please alert a developer or the owner");
                return true;
            }
        }
        try
        {
            Component component = this.execute(sender, isConsole(sender) ? null : (Player) sender, args);
            if (component != null)
            {
                send(sender, component);
            }
        }
        catch (PlayerNotFoundException | CommandFailException | ConsoleOnlyException |
               ConsoleMustDefinePlayerException | PlayerNotBannedException | NumberFormatException ex)
        {
            send(sender, PlexUtils.mmDeserialize(ex.getMessage()));
        }
        return true;
    }

    /**
     * Checks if the String given is a matching command
     *
     * @param label The String to check
     * @return true if the string is a command name or alias
     */
    private boolean matches(String label)
    {
        if (params.aliases().split(",").length > 0)
        {
            for (String alias : params.aliases().split(","))
            {
                if (alias.equalsIgnoreCase(label) || getName().equalsIgnoreCase(label))
                {
                    return true;
                }
            }
        }
        else if (params.aliases().split(",").length < 1)
        {
            return getName().equalsIgnoreCase(label);
        }
        return false;
    }

    /**
     * Gets a PlexPlayer from Player object
     *
     * @param player The player object
     * @return PlexPlayer Object
     * @see PlexPlayer
     */
    protected PlexPlayer getPlexPlayer(@NotNull Player player)
    {
        return DataUtils.getPlayer(player.getUniqueId());
    }

    /**
     * Sends a message to an Audience
     *
     * @param audience The Audience to send the message to
     * @param s        The message to send
     */
    protected void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    /**
     * Sends a message to an Audience
     *
     * @param audience  The Audience to send the message to
     * @param component The Component to send
     */
    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    /**
     * Checks whether a sender has enough permissions or is high enough a rank
     *
     * @param sender     A CommandSender
     * @param rank       The rank to check (if the server is using ranks)
     * @param permission The permission to check (if the server is using permissions)
     * @return true if the sender has enough permissions
     * @see Rank
     */
    protected boolean checkRank(CommandSender sender, Rank rank, String permission)
    {
        if (!isConsole(sender))
        {
            return checkRank((Player) sender, rank, permission);
        }
        /*if (!sender.getName().equalsIgnoreCase("console"))
        {
            PlexPlayer plexPlayer = DataUtils.getPlayer(sender.getName());
            if (plugin.getSystem().equalsIgnoreCase("ranks"))
            {
                if (!plexPlayer.getRankFromString().isAtLeast(rank))
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionRank", ChatColor.stripColor(rank.getLoginMessage())));
                }
                if (rank.isAtLeast(Rank.ADMIN) && !plexPlayer.isAdminActive())
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionRank", ChatColor.stripColor(rank.getLoginMessage())));
                }
            }
            else if (plugin.getSystem().equalsIgnoreCase("permissions"))
            {
                if (!perms.permission().isEmpty() && !plugin.getPermissions().playerHas(null, Bukkit.getOfflinePlayer(plexPlayer.getName()), perms.permission()))
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionNode", permission));
                }
            }
        }*/
        return true;
    }

    /**
     * Checks whether a sender has enough permissions or is high enough a rank
     *
     * @param sender     A CommandSender
     * @param rank       The rank to check (if the server is using ranks)
     * @param permission The permission to check (if the server is using permissions)
     * @return true if the sender has enough permissions
     * @see Rank
     */
    protected boolean silentCheckRank(CommandSender sender, Rank rank, String permission)
    {
        PlexLog.debug("Checking {0} with {1}", sender.getName(), permission);
        if (!isConsole(sender))
        {
            return silentCheckRank((Player) sender, rank, permission);
        }
        /*if (!sender.getName().equalsIgnoreCase("console"))
        {
            PlexPlayer plexPlayer = DataUtils.getPlayer(sender.getName());
            if (plugin.getSystem().equalsIgnoreCase("ranks"))
            {
                if (!plexPlayer.getRankFromString().isAtLeast(rank))
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionRank", ChatColor.stripColor(rank.getLoginMessage())));
                }
                if (rank.isAtLeast(Rank.ADMIN) && !plexPlayer.isAdminActive())
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionRank", ChatColor.stripColor(rank.getLoginMessage())));
                }
            }
            else if (plugin.getSystem().equalsIgnoreCase("permissions"))
            {
                if (!perms.permission().isEmpty() && !plugin.getPermissions().playerHas(null, Bukkit.getOfflinePlayer(plexPlayer.getName()), perms.permission()))
                {
                    throw new CommandFailException(PlexUtils.messageString("noPermissionNode", permission));
                }
            }
        }*/
        return true;
    }

    /**
     * Checks whether a player has enough permissions or is high enough a rank
     *
     * @param player     The player object
     * @param rank       The rank to check (if the server is using ranks)
     * @param permission The permission to check (if the server is using permissions)
     * @return true if the sender has enough permissions
     * @see Rank
     */
    protected boolean checkRank(Player player, Rank rank, String permission)
    {
        if (player instanceof ConsoleCommandSender)
        {
            return true;
        }
        PlexPlayer plexPlayer = getPlexPlayer(player);
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (!plexPlayer.getRankFromString().isAtLeast(rank) || (rank.isAtLeast(Rank.ADMIN) && !plexPlayer.isAdminActive()))
            {
                throw new CommandFailException(PlexUtils.messageString("noPermissionRank", ChatColor.stripColor(rank.getLoginMessage())));
            }
        }
        else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            if (!permission.isEmpty() && !player.hasPermission(permission))
            {
                throw new CommandFailException(PlexUtils.messageString("noPermissionNode", permission));
            }
        }
        return true;
    }

    protected boolean silentCheckRank(Player player, Rank rank, String permission)
    {
        PlexPlayer plexPlayer = getPlexPlayer(player);
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            return rank.isAtLeast(Rank.ADMIN) ? plexPlayer.isAdminActive() && plexPlayer.getRankFromString().isAtLeast(rank) : plexPlayer.getRankFromString().isAtLeast(rank);
        }
        else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            return !permission.isEmpty() && player.hasPermission(permission);
        }
        return false;
    }

/*    *//**
     * Checks whether a sender has enough permissions or is high enough a rank
     *
     * @param sender     The player object
     * @param rank       The rank to check (if the server is using ranks)
     * @param permission The permission to check (if the server is using permissions)
     * @return true if the sender has enough permissions
     * @see Rank
     *//*
    protected boolean silentCheckRank(CommandSender sender, Rank rank, String permission)
    {
        if (!isConsole(sender))
        {
            return silentCheckRank((Player) sender, rank, permission);
        }
        return true;
    }

    *//**
     * Checks whether a player has enough permissions or is high enough a rank
     *
     * @param player     The player object
     * @param rank       The rank to check (if the server is using ranks)
     * @param permission The permission to check (if the server is using permissions)
     * @return true if the sender has enough permissions
     * @see Rank
     *//*
    protected boolean silentCheckRank(Player player, Rank rank, String permission)
    {
        PlexPlayer plexPlayer = getPlexPlayer(player);
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            return rank.isAtLeast(Rank.ADMIN) ? plexPlayer.isAdminActive() && plexPlayer.getRankFromString().isAtLeast(rank) : plexPlayer.getRankFromString().isAtLeast(rank);
        }
        else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            return !permission.isEmpty() && player.hasPermission(permission);
        }
        return true;
    }*/

    /**
     * Checks if a player is an admin
     *
     * @param plexPlayer The PlexPlayer object
     * @return true if the player is an admin
     * @see PlexPlayer
     */
    protected boolean isAdmin(PlexPlayer plexPlayer)
    {
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a sender is an admin
     *
     * @param sender A command sender
     * @return true if the sender is an admin or if console
     */
    protected boolean isAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return true;
        }
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return plugin.getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a username is an admin
     *
     * @param name The username
     * @return true if the username is an admin
     */
    protected boolean isAdmin(String name)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(name);
        return plugin.getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a sender is a senior admin
     *
     * @param sender A command sender
     * @return true if the sender is a senior admin or if console
     */
    protected boolean isSeniorAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return true;
        }
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return plugin.getRankManager().isSeniorAdmin(plexPlayer);
    }

    /**
     * Gets the UUID of the sender
     *
     * @param sender A command sender
     * @return A unique ID or null if the sender is console
     * @see UUID
     */
    protected UUID getUUID(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return null;
        }
        return player.getUniqueId();
    }

    /**
     * The plugin
     *
     * @return The instance of the plugin
     * @see Plex
     */
    @Override
    public @NotNull Plex getPlugin()
    {
        return plugin;
    }

    /**
     * Checks whether a sender is console
     *
     * @param sender A command sender
     * @return true if the sender is console
     */
    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    /**
     * Converts a message entry from the "messages.yml" to a Component
     *
     * @param s       The message entry
     * @param objects Any objects to replace in order
     * @return A Kyori Component
     */
    protected Component messageComponent(String s, Object... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    /**
     * Converts a message entry from the "messages.yml" to a Component
     *
     * @param s       The message entry
     * @param objects Any objects to replace in order
     * @return A Kyori Component
     */
    protected Component messageComponent(String s, Component... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    /**
     * Converts a message entry from the "messages.yml" to a String
     *
     * @param s       The message entry
     * @param objects Any objects to replace in order
     * @return A String
     */
    protected String messageString(String s, Object... objects)
    {
        return PlexUtils.messageString(s, objects);
    }

    /**
     * Converts usage to a Component
     *
     * @return A Kyori Component stating the usage
     */
    protected Component usage()
    {
        return Component.text("Correct Usage: ").color(NamedTextColor.YELLOW).append(componentFromString(this.getUsage()).color(NamedTextColor.GRAY));
    }

    /**
     * Converts usage to a Component
     * <p>
     * s The usage to convert
     *
     * @return A Kyori Component stating the usage
     */
    protected Component usage(String s)
    {
        return Component.text("Correct Usage: ").color(NamedTextColor.YELLOW).append(componentFromString(s).color(NamedTextColor.GRAY));
    }

    protected Player getNonNullPlayer(String name)
    {
        try
        {
            UUID uuid = UUID.fromString(name);
            return Bukkit.getPlayer(uuid);
        }
        catch (IllegalArgumentException ignored)
        {

        }

        Player player = Bukkit.getPlayer(name);
        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        return player;
    }

    protected PlexPlayer getOnlinePlexPlayer(String name)
    {
        Player player = getNonNullPlayer(name);
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(player.getUniqueId());
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    protected PlexPlayer getOfflinePlexPlayer(UUID uuid)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(uuid);
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    protected World getNonNullWorld(String name)
    {
        World world = Bukkit.getWorld(name);
        if (world == null)
        {
            throw new CommandFailException(PlexUtils.messageString("worldNotFound"));
        }
        return world;
    }

    /**
     * Converts a String to a legacy Kyori Component
     *
     * @param s The String to convert
     * @return A Kyori component
     */
    protected Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    protected Component noColorComponentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    /**
     * Converts a String to a MiniMessage Component
     *
     * @param s The String to convert
     * @return A Kyori Component
     */
    protected Component mmString(String s)
    {
        return PlexUtils.mmDeserialize(s);
    }

    public Rank getLevel()
    {
        return level;
    }

    public CommandMap getMap()
    {
        return Plex.get().getServer().getCommandMap();
    }
}
