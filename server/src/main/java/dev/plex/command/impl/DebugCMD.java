package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.menu.impl.MaterialMenu;
import dev.plex.util.GameRuleUtil;
import dev.plex.util.PlexLog;

import java.util.Arrays;
import java.util.Locale;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "pdebug", description = "Plex's debug command", usage = "/<command> <aliases <command> | redis-reset <player> | gamerules>")
@CommandPermissions(permission = "plex.debug")
public class DebugCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("redis-reset")
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context, "redis-reset", string(context, "player")))));
        command.then(literal("gamerules")
                .executes(context -> executeCommand(context, "gamerules")));
        command.then(literal("aliases")
                .then(word("command")
                        .executes(context -> executeCommand(context, "aliases", string(context, "command")))));
        command.then(literal("pagination")
                .executes(context -> executeCommand(context, "pagination")));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }
        if (args[0].equalsIgnoreCase("redis-reset"))
        {
            if (args.length == 2)
            {
                Player player = getNonNullPlayer(args[1]);
                if (plugin.getRedisConnection().query(jedis -> jedis.exists(player.getUniqueId().toString())))
                {
                    plugin.getRedisConnection().execute(jedis -> jedis.del(player.getUniqueId().toString()));
                    return messageComponent("redisResetSuccessful", player.getName());
                }
                return messageComponent("redisResetPlayerNotFound");
            }
        }
        if (args[0].equalsIgnoreCase("gamerules"))
        {
            for (World world : Bukkit.getWorlds())
            {
                GameRuleUtil.commitGlobalGameRules(plugin, world);
                PlexLog.log("Set global gamerules for world: " + world.getName());
            }
            for (String world : plugin.config.getConfigurationSection("worlds").getKeys(false))
            {
                World bukkitWorld = Bukkit.getWorld(world);
                if (bukkitWorld != null)
                {
                    GameRuleUtil.commitSpecificGameRules(plugin, bukkitWorld);
                    PlexLog.log("Set specific gamerules for world: " + world.toLowerCase(Locale.ROOT));
                }
            }
            return messageComponent("reappliedGamerules");
        }
        if (args[0].equalsIgnoreCase("aliases"))
        {
            if (args.length == 2)
            {
                String commandName = args[1];
                PlexCommand plexCommand = plugin.getCommandHandler().getCommand(commandName);
                if (plexCommand != null)
                {
                    return messageComponent("commandAliases", commandName, Arrays.toString(plexCommand.getAliases().toArray(new String[0])));
                }
                Command command = plugin.getServer().getCommandMap().getCommand(commandName);
                if (command == null)
                {
                    return messageComponent("commandNotFound");
                }
                return messageComponent("commandAliases", commandName, Arrays.toString(command.getAliases().toArray(new String[0])));
            }
        }
        if (args[0].equalsIgnoreCase("pagination"))
        {
            if (playerSender == null)
            {
                return messageComponent("noPermissionConsole");
            }
            new MaterialMenu().open(playerSender);
            return null;
        }
        return usage();
    }

}
