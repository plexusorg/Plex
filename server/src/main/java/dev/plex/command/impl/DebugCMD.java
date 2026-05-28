package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
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

public class DebugCMD extends ServerCommand
{
    public DebugCMD()
    {
        super(command("pdebug")
            .description("Plex's debug command")
            .usage("/<command> <aliases <command> | redis | redis-reset <player> | gamerules>")
            .permission("plex.debug")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("redis")
                .executes(context -> executeCommand(context, "redis")));
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
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }
        if (args[0].equalsIgnoreCase("redis"))
        {
            if (!plugin.getRedisConnection().isEnabled())
            {
                throw new CommandFailException("&cRedis is not enabled.");
            }
            plugin.getRedisConnection().execute(jedis -> jedis.set("test", "123"));
            context.send(sender, "Set test to 123. Now outputting key test...");
            String test = plugin.getRedisConnection().query(jedis -> jedis.get("test"));
            context.send(sender, test);
            return null;
        }
        if (args[0].equalsIgnoreCase("redis-reset"))
        {
            if (args.length == 2)
            {
                Player player = context.getNonNullPlayer(args[1]);
                if (plugin.getRedisConnection().query(jedis -> jedis.exists(player.getUniqueId().toString())))
                {
                    plugin.getRedisConnection().execute(jedis -> jedis.del(player.getUniqueId().toString()));
                    return context.messageComponent("redisResetSuccessful", player.getName());
                }
                return context.messageComponent("redisResetPlayerNotFound");
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
            return context.messageComponent("reappliedGamerules");
        }
        if (args[0].equalsIgnoreCase("aliases"))
        {
            if (args.length == 2)
            {
                String commandName = args[1];
                PlexCommand plexCommand = plugin.getCommandHandler().getCommand(commandName);
                if (plexCommand != null)
                {
                    return context.messageComponent("commandAliases", commandName, Arrays.toString(plexCommand.getAliases().toArray(new String[0])));
                }
                Command command = plugin.getServer().getCommandMap().getCommand(commandName);
                if (command == null)
                {
                    return context.messageComponent("commandNotFound");
                }
                return context.messageComponent("commandAliases", commandName, Arrays.toString(command.getAliases().toArray(new String[0])));
            }
        }
        if (args[0].equalsIgnoreCase("pagination"))
        {
            if (playerSender == null)
            {
                return context.messageComponent("noPermissionConsole");
            }
            new MaterialMenu().open(playerSender);
            return null;
        }
        return context.usage();
    }

}
