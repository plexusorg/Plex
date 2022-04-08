package dev.plex.listener.impl;

import com.google.gson.Gson;
import dev.plex.cache.DataUtils;
import dev.plex.cache.player.PlayerCache;
import dev.plex.command.blocking.BlockedCommand;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.services.impl.CommandBlockerService;
import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl -> PlayerCache.getPlexPlayer(pl.getUniqueId()).isCommandSpy()).forEach(pl ->
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
        String commandName = event.getMessage().split(" ")[0].replace("/", "");
        String arguments = StringUtils.normalizeSpace(event.getMessage().replace(event.getMessage().split(" ")[0], ""));
        PlexLog.debug("Checking Command: {0} with args {1}", commandName, arguments);
        AtomicReference<BlockedCommand> cmdRef = new AtomicReference<>();
        CommandBlockerService.getBLOCKED_COMMANDS().stream().filter(blockedCommand -> blockedCommand.getCommand() != null).findFirst().ifPresent(blockedCommand ->
        {
            if (event.getMessage().replace("/", "").toLowerCase(Locale.ROOT).startsWith(blockedCommand.getCommand().toLowerCase(Locale.ROOT)))
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
                    PlexLog.debug("Player attempted to use a blocked command with an alias.");
                    cmdRef.set(blockedCommand);
                    return;
                }
            }
        });
        if (cmdRef.get() == null)
        {
            CommandBlockerService.getBLOCKED_COMMANDS().forEach(blockedCommand ->
            {
                if (blockedCommand.getRegex() != null)
                {
                    Pattern pattern = Pattern.compile(blockedCommand.getRegex());
                    Matcher matcher = pattern.matcher(event.getMessage().replace("/", ""));
                    if (matcher.find())
                    {
                        PlexLog.debug("Found blocked regexed command");
                        cmdRef.set(blockedCommand);
                    }
                }
            });
        }
        if (cmdRef.get() != null)
        {
            BlockedCommand cmd = cmdRef.get();
            if (cmd.getRequiredLevel().equalsIgnoreCase("e"))
            {
                event.setCancelled(true);
                event.getPlayer().sendMessage(Component.text(cmd.getMessage()).color(NamedTextColor.GRAY));
                return;
            }
            if (cmd.getRequiredLevel().equalsIgnoreCase("a"))
            {
                if (plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN) && plexPlayer.isAdminActive())
                {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text(cmd.getMessage()).color(NamedTextColor.GRAY));
                    return;
                }
            }
            if (cmd.getRequiredLevel().equalsIgnoreCase("s"))
            {
                if (plexPlayer.getRankFromString().isAtLeast(Rank.SENIOR_ADMIN) && plexPlayer.isAdminActive())
                {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Component.text(cmd.getMessage()).color(NamedTextColor.GRAY));
                    return;
                }
            }
        }

    }
}
