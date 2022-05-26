package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.blocking.BlockedCommand;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.services.impl.CommandBlockerService;
import dev.plex.util.PlexLog;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl -> plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId()).isCommandSpy() && hasCommandSpy(plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId()))).forEach(pl ->
        {
            Player player = event.getPlayer();
            String command = event.getMessage();
            if (pl != player)
            {
                pl.sendMessage(ChatColor.GRAY + player.getName() + ": " + command);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandBlocking(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
        String commandName = StringUtils.normalizeSpace(event.getMessage()).split(" ")[0].replaceFirst("/", "");
        String arguments = StringUtils.normalizeSpace(StringUtils.normalizeSpace(event.getMessage()).replace(event.getMessage().split(" ")[0], ""));
        PlexLog.debug("Checking Command: {0} with args {1}", commandName, arguments);
        AtomicReference<BlockedCommand> cmdRef = new AtomicReference<>();
        PlexLog.debug("Blocked Commands List: " + CommandBlockerService.getBLOCKED_COMMANDS().size());
        CommandBlockerService.getBLOCKED_COMMANDS().stream().filter(blockedCommand -> blockedCommand.getCommand() != null).forEach(blockedCommand ->
        {
            boolean matches = true;
            String[] args = blockedCommand.getCommand().split(" ");
            String[] cmdArgs = event.getMessage().replaceFirst("/", "").split(" ");
            for (int i = 0; i < args.length; i++)
            {
                if (i + 1 > cmdArgs.length)
                {
                    matches = false;
                    break;
                }
                if (!args[i].equalsIgnoreCase(cmdArgs[i]))
                {
                    matches = false;
                    break;
                }
            }
            if (matches)
            {
                PlexLog.debug("Used blocked command exactly matched");
                cmdRef.set(blockedCommand);
                return;
            }
            if (blockedCommand.getCommandAliases().stream().anyMatch(s -> s.equalsIgnoreCase(commandName)))
            {
                PlexLog.debug("Found a command name in a blocked command alias, checking arguments now.");
                String[] commandArgs = blockedCommand.getCommand().split(" ");
                if (arguments.toLowerCase(Locale.ROOT).startsWith(StringUtils.join(commandArgs, " ", 1, commandArgs.length).toLowerCase(Locale.ROOT)))
                {
                    PlexLog.debug("Player attempted to use a blocked command with alias of normal command: " + blockedCommand.getCommand());
                    cmdRef.set(blockedCommand);
                }
            }
        });
        if (cmdRef.get() == null)
        {
            CommandBlockerService.getBLOCKED_COMMANDS().forEach(blockedCommand ->
            {
                if (blockedCommand.getRegex() != null)
                {
                    Pattern pattern = Pattern.compile(blockedCommand.getRegex(), Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(event.getMessage().replaceFirst("/", ""));
                    if (matcher.find())
                    {
                        PlexLog.debug("Player attempted to use a blocked regex");
                        cmdRef.set(blockedCommand);
                    }
                }
            });
        }
        if (cmdRef.get() != null)
        {
            BlockedCommand cmd = cmdRef.get();
            switch (cmd.getRequiredLevel().toLowerCase(Locale.ROOT))
            {
                case "e" ->
                {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(cmd.getMessage());
                }
                case "a" ->
                {
                    if (plexPlayer.isAdminActive() && plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN))
                    {
                        return;
                    }
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(cmd.getMessage());
                }
                case "s" ->
                {
                    if (plexPlayer.isAdminActive() && plexPlayer.getRankFromString().isAtLeast(Rank.SENIOR_ADMIN))
                    {
                        return;
                    }
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(cmd.getMessage());
                }
            }
        }
    }

    private boolean hasCommandSpy(PlexPlayer plexPlayer)
    {
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            return plexPlayer.isAdminActive();
        }
        else if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            return plexPlayer.getPlayer().hasPermission("plex.commandspy");
        }
        return false;
    }
}
