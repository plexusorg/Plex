package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.command.blocking.BlockedCommand;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.services.impl.CommandBlockerService;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl ->
        {
            PlexPlayer player = plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId());
            return player.isCommandSpy() && hasCommandSpy(plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId()));
        }).forEach(pl ->
        {
            Player player = event.getPlayer();
            String command = event.getMessage();
            if (!pl.getUniqueId().equals(player.getUniqueId()))
            {
                pl.sendMessage(Component.text(player.getName() + ": " + command).color(NamedTextColor.GRAY));
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommandBlocking(PlayerCommandPreprocessEvent event)
    {
        String command = "/" + event.getMessage().replaceFirst("/", "").trim();
        Player player = event.getPlayer();
        if (Plex.get().getPermissions() != null && Plex.get().getPermissions().has(player, "plex.commandblocker.bypass")) return;
        PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
        String commandName = StringUtils.normalizeSpace(command).split(" ")[0].replaceFirst("/", "");
        String arguments = StringUtils.normalizeSpace(StringUtils.normalizeSpace(command).replace(command.split(" ")[0], ""));
        PlexLog.debug("Checking Command: {0} with args {1}", commandName, arguments);
        AtomicReference<BlockedCommand> cmdRef = new AtomicReference<>();
        PlexLog.debug("Blocked Commands List: " + CommandBlockerService.getBLOCKED_COMMANDS().size());
        CommandBlockerService.getBLOCKED_COMMANDS().stream().filter(blockedCommand -> blockedCommand.getCommand() != null).forEach(blockedCommand ->
        {
            boolean matches = true;
            String[] args = blockedCommand.getCommand().split(" ");
            String[] cmdArgs = command.replaceFirst("/", "").split(" ");
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
                    Matcher matcher = pattern.matcher(command.replaceFirst("/", ""));
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
