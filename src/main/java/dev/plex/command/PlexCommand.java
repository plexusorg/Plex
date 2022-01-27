package dev.plex.command;

import com.google.common.collect.ImmutableList;
import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.cache.PlayerCache;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

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
        setUsage(params.usage().replace("<command>", name));
        if (params.aliases().split(",").length > 0)
        {
            setAliases(Arrays.asList(params.aliases().split(",")));
        }
        this.level = perms.level();
        this.commandSource = perms.source();

        getMap().register("plex", this);
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
        if (!matches(label))
        {
            return false;
        }

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
            Player player = (Player)sender;

            this.sender = new CommandSource(player);
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (!plexPlayer.getRankFromString().isAtLeast(getLevel()))
            {
                sender.sendMessage(tl("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMSG())));
                return true;
            }
        }
        try
        {
            this.sender = new CommandSource(sender);
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
        catch (ConsoleMustDefinePlayerException ex)
        {
            send(tl("consoleMustDefinePlayer"));
        }
        catch (ConsoleOnlyException ex)
        {
            send(tl("consoleOnly"));
        }
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
    {
        if (!matches(alias))
        {
            return ImmutableList.of();
        }
        if (sender instanceof Player player)
        {

            this.sender = new CommandSource(player);

            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (plexPlayer.getRankFromString().isAtLeast(getLevel()))
            {
                return onTabComplete(this.sender, args);
            }
            else
            {
                return ImmutableList.of();
            }
        }
        else
        {
            this.sender = new CommandSource(sender);
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
        }
        else if (params.aliases().split(",").length < 1)
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

    protected boolean isAdmin(PlexPlayer plexPlayer)
    {
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    protected boolean isAdmin(String name)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(name);
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    protected boolean isConsole()
    {
        return !(sender instanceof Player);
    }

    protected String tl(String s, Object... objects)
    {
        return PlexUtils.tl(s, objects);
    }

    protected String usage(String s)
    {
        return ChatColor.YELLOW + "Correct Usage: " + ChatColor.GRAY + s;
    }

    protected void send(String s)
    {
        if (sender == null)
        {
            return;
        }
        send(s, sender);
    }

    protected Player getNonNullPlayer(String name)
    {
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
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayer(player.getUniqueId());
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    protected PlexPlayer getOfflinePlexPlayer(UUID uuid)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayer(uuid);
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
            throw new CommandFailException(tl("worldNotFound"));
        }
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
