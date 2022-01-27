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
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public abstract class PlexCommand extends Command
{
    protected static Plex plugin = Plex.get();

    private final CommandParameters params;
    private final CommandPermissions perms;

    private final Rank level;
    private final RequiredCommandSource commandSource;

    public PlexCommand()
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

        getMap().register("plex", this);
    }

    protected abstract Component execute(CommandSender sender, String[] args);


    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args)
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
                send(sender, tl("noPermissionConsole"));
                return true;
            }
            Player player = (Player)sender;

            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
            if (!plexPlayer.getRankFromString().isAtLeast(getLevel()))
            {
                send(sender, tl("noPermissionRank", ChatColor.stripColor(getLevel().getLoginMSG())));
                return true;
            }
        }
        try
        {
            Component component = this.execute(sender, args);
            if (component != null)
            {
                send(sender, component);
            }
        }
        catch (CommandArgumentException ex)
        {
            send(sender, getUsage().replace("<command>", getLabel()));
        }
        catch (PlayerNotFoundException | CommandFailException ex)
        {
            send(sender, ex.getMessage());
        }
        catch (ConsoleMustDefinePlayerException ex)
        {
            send(sender, tl("consoleMustDefinePlayer"));
        }
        catch (ConsoleOnlyException ex)
        {
            send(sender, tl("consoleOnly"));
        }
        return true;
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

    protected PlexPlayer getPlexPlayer(@NotNull Player player)
    {
        return DataUtils.getPlayer(player.getUniqueId());
    }

    protected void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }


    protected boolean isAdmin(PlexPlayer plexPlayer)
    {
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    protected boolean isAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player)) return true;
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    protected boolean isAdmin(String name)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(name);
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    protected boolean isSeniorAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player)) return true;
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return Plex.get().getRankManager().isSeniorAdmin(plexPlayer);
    }

    protected UUID getUUID(CommandSender sender)
    {
        if (!(sender instanceof Player player)) return null;
        return player.getUniqueId();
    }


    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    protected Component tl(String s, Object... objects)
    {
        return componentFromString(PlexUtils.tl(s, objects));
    }

    protected Component usage(String s)
    {
        return componentFromString(ChatColor.YELLOW + "Correct Usage: " + ChatColor.GRAY + s);
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
            throw new CommandFailException(PlexUtils.tl("worldNotFound"));
        }
        return world;
    }

    protected Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
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
