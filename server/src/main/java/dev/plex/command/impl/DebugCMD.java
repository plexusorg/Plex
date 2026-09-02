package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.menu.dialog.MaterialDialog;
import dev.plex.util.GameRuleUtil;
import dev.plex.util.PlexLog;

import java.util.Arrays;

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
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }
        return switch (args[0])
        {
            case "redis" -> redis(context);
            case "redis-reset" -> resetRedis(context, args);
            case "gamerules" -> gamerules(context);
            case "aliases" -> aliases(context, args);
            case "pagination" -> pagination(context);
            default -> context.usage();
        };
    }

    private Component redis(ServerCommandContext context)
    {
        if (!plugin.getRedisConnection().isEnabled())
        {
            throw new CommandFailException("&cRedis is not enabled.");
        }
        plugin.getRedisConnection().execute(jedis -> jedis.set("test", "123"));
        context.send(context.sender(), "Set test to 123. Now outputting key test...");
        String value = plugin.getRedisConnection().query(jedis -> jedis.get("test"));
        context.send(context.sender(), value);
        return null;
    }

    private Component resetRedis(ServerCommandContext context, String[] args)
    {
        if (args.length != 2)
        {
            return context.usage();
        }
        Player player = context.getNonNullPlayer(args[1]);
        String key = player.getUniqueId().toString();
        if (!plugin.getRedisConnection().query(jedis -> jedis.exists(key)))
        {
            return context.messageComponent("redisResetPlayerNotFound");
        }
        plugin.getRedisConnection().execute(jedis -> jedis.del(key));
        return context.messageComponent("redisResetSuccessful", player.getName());
    }

    private Component gamerules(ServerCommandContext context)
    {
        for (World world : Bukkit.getWorlds())
        {
            GameRuleUtil.commitGlobalGameRules(plugin, world);
            PlexLog.log("Set global gamerules for world: " + world.getName());
        }
        for (String world : plugin.worlds.getConfigurationSection("worlds").getKeys(false))
        {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld != null)
            {
                GameRuleUtil.commitSpecificGameRules(plugin, bukkitWorld);
                    PlexLog.log("Set specific gamerules for world: " + world);
            }
        }
        return context.messageComponent("reappliedGamerules");
    }

    private Component aliases(ServerCommandContext context, String[] args)
    {
        if (args.length != 2)
        {
            return context.usage();
        }
        String commandName = args[1];
        PlexCommand plexCommand = plugin.getCommandHandler().getCommand(commandName);
        if (plexCommand != null)
        {
            return context.messageComponent("commandAliases", commandName, Arrays.toString(plexCommand.getAliases().toArray(new String[0])));
        }
        Command command = plugin.getServer().getCommandMap().getCommand(commandName);
        return command == null ? context.messageComponent("commandNotFound")
                : context.messageComponent("commandAliases", commandName, Arrays.toString(command.getAliases().toArray(new String[0])));
    }

    private Component pagination(ServerCommandContext context)
    {
        Player player = context.player();
        if (player == null)
        {
            return context.messageComponent("noPermissionConsole");
        }
        new MaterialDialog().open(player);
        return null;
    }

}
