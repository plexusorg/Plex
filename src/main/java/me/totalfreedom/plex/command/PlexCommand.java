package me.totalfreedom.plex.command;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.CommandArgumentException;
import me.totalfreedom.plex.command.exception.CommandFailException;
import me.totalfreedom.plex.command.exception.PlayerNotFoundException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexLog;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static me.totalfreedom.plex.util.PlexUtils.tl;

public abstract class PlexCommand extends Command implements TabExecutor, IPlexCommand
{
    protected static Plex plugin = Plex.get();

    private final CommandParameters params;
    private final CommandPermissions perms;

    private final Rank level;
    private CommandSource sender;
    private final RequiredCommandSource commandSource;

    public PlexCommand(String name)
    {
        super(name);
        this.params = getClass().getAnnotation(CommandParameters.class);
        this.perms = getClass().getAnnotation(CommandPermissions.class);

        setName(name);
        setLabel(name);
        setDescription(params.description());
        setUsage(params.usage());
        if (params.aliases().split(",").length > 0)
        {
            setAliases(Arrays.asList(params.aliases().split(",")));
        }
        this.level = perms.level();
        this.commandSource = perms.source();

        getMap().register("", this);
    }


    @Override
    public boolean execute(CommandSender sender, String label, String[] args)
    {
        onCommand(sender, this, label, args);
        return true;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!matches(label)) return false;
        if (this.sender == null)
            this.sender = new CommandSource(sender);
        PlexLog.log(this.sender.getSender().getName());
        if (commandSource == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            sender.sendMessage(tl("noPermissionInGame"));
            return true;
        }
        if (commandSource == RequiredCommandSource.IN_GAME)
        {
            if (sender instanceof ConsoleCommandSender)
            {
                sender.sendMessage(tl("noPermissionConsole"));
                return true;
            }
            Player player = (Player) sender;
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (!plexPlayer.getRankFromString().isAtLeast(getLevel()))
            {
                sender.sendMessage(tl("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMSG())));
                return true;
            }
        }
        try
        {
            execute(this.sender, args);
        }
        catch (CommandArgumentException ex)
        {
            send(getUsage().replace("<command>", getLabel()));
        }
        catch (PlayerNotFoundException | CommandFailException ex)
        {
            send(ex.getMessage());
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
    {
        if (!matches(alias)) return ImmutableList.of();
        if (this.sender == null)
            this.sender = new CommandSource(sender);
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (plexPlayer.getRankFromString().isAtLeast(getLevel()))
            {
                return onTabComplete(this.sender, args);
            } else {
                return ImmutableList.of();
            }
        } else {
            return onTabComplete(this.sender, args);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
    {
        return tabComplete(sender, label, args);
    }

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
        } else if (params.aliases().split(",").length < 1)
        {
            return getName().equalsIgnoreCase(label);
        }
        return false;
    }

    protected void send(String s, CommandSource sender)
    {
        sender.send(s);
    }

    protected void send(String s, Player player)
    {
        player.sendMessage(s);
    }

    protected String usage(String s)
    {
        return ChatColor.YELLOW + "Correct Usage: " + ChatColor.GRAY + s;
    }

    protected void send(String s)
    {
        if (sender == null)
            return;
        send(s, sender);
    }

    protected Player getNonNullPlayer(String name)
    {
        Player player = Bukkit.getPlayer(name);
        if (player == null)
            throw new PlayerNotFoundException();
        return player;
    }

    protected PlexPlayer getOnlinePlexPlayer(String name)
    {
        Player player = getNonNullPlayer(name);
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayer(player.getUniqueId());
        if (plexPlayer == null)
            throw new PlayerNotFoundException();
        return plexPlayer;
    }

    protected PlexPlayer getOfflinePlexPlayer(UUID uuid)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayer(uuid);
        if (plexPlayer == null)
            throw new PlayerNotFoundException();
        return plexPlayer;
    }

    protected World getNonNullWorld(String name)
    {
        World world = Bukkit.getWorld(name);
        if (world == null)
            throw new CommandFailException(tl("worldNotFound"));
        return world;
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
