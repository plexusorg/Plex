package me.totalfreedom.plex.command;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.command.annotations.CommandParameters;
import me.totalfreedom.plex.command.annotations.CommandPermissions;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public abstract class PlexCommand extends Command implements TabExecutor, IPlexCommand
{
    private final CommandParameters params;
    private final CommandPermissions perms;

    private final Rank level;
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
        if (commandSource == RequiredCommandSource.CONSOLE)
        {
            if (sender instanceof Player)
            {
                //TODO: Enter console only msg
                return true;
            }
            execute(sender, args);
            return true;

        } else if (commandSource == RequiredCommandSource.IN_GAME)
        {
            if (!(sender instanceof Player))
            {
                //TODO: Enter player only msg
                return true;
            }

            Player player = (Player) sender;
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (!plexPlayer.getRankFromString().isAtleast(getLevel()))
            {
                //TODO: Enter <insert level> only and higher msg
                return true;
            }
            execute(sender, args);
            return true;
        } else {
            if (!(sender instanceof Player))
            {
                execute(sender, args);
                return true;
            } else {
                Player player = (Player) sender;
                PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
                if (!plexPlayer.getRankFromString().isAtleast(getLevel()))
                {
                    //TODO: Enter <insert level> only and higher msg
                    return true;
                }
                execute(sender, args);
                return true;
            }
        }
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args)
    {
        if (!matches(alias)) return ImmutableList.of();
        if (sender instanceof Player)
        {
            Player player = (Player) sender;
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (plexPlayer.getRankFromString().isAtleast(getLevel()))
            {
                return onTabComplete(sender, args);
            } else {
                return ImmutableList.of();
            }
        } else {
            return onTabComplete(sender, args);
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


    public Rank getLevel()
    {
        return level;
    }

    public CommandMap getMap()
    {
        return Plex.get().getServer().getCommandMap();
    }
}
