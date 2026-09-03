package dev.plex.command.impl;

import org.bukkit.Bukkit;

import dev.plex.util.PlexUtils;
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
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(literal("redis")
                .executes(context -> executeCommand(context, this::redis)));
        command.then(literal("redis-reset")
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context,
                                commandContext -> resetRedis(commandContext, string(context, "player"))))));
        command.then(literal("gamerules")
                .executes(context -> executeCommand(context, this::gamerules)));
        command.then(literal("aliases")
                .then(word("command")
                        .executes(context -> executeCommand(context,
                                commandContext -> aliases(commandContext, string(context, "command"))))));
        command.then(literal("pagination")
                .executes(context -> executeCommand(context, this::pagination)));
    }

    private Component redis(ServerCommandContext context)
    {
        if (!plugin.getRedisConnection().isEnabled())
        {
            throw new CommandFailException("&cRedis is not enabled.");
        }
        plugin.getRedisConnection().queryAsync(jedis ->
        {
            jedis.set("test", "123");
            return jedis.get("test");
        }).whenComplete((value, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Redis debug command failed", failure);
                context.sender().sendMessage(Component.text("Redis operation failed; check the server logs."));
                return;
            }
            context.sender().sendMessage("Set test to 123. Now outputting key test...");
            context.sender().sendMessage(value);
        });
        return null;
    }

    private Component resetRedis(ServerCommandContext context, String playerName)
    {
        Player player = getNonNullPlayer(playerName);
        String key = player.getUniqueId().toString();
        String name = player.getName();
        plugin.getRedisConnection().queryAsync(jedis ->
        {
            if (!jedis.exists(key))
            {
                return false;
            }
            jedis.del(key);
            return true;
        }).whenComplete((removed, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Redis reset failed for " + key, failure);
                context.sender().sendMessage(Component.text("Redis operation failed; check the server logs."));
                return;
            }
            context.sender().sendMessage(removed
                    ? PlexUtils.messageComponent("redisResetSuccessful", name)
                    : PlexUtils.messageComponent("redisResetPlayerNotFound"));
        });
        return null;
    }

    private Component gamerules(ServerCommandContext context)
    {
        Runnable apply = () ->
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
            context.sender().sendMessage(PlexUtils.messageComponent("reappliedGamerules"));
        };
        if (context.player() == null) apply.run();
        else Bukkit.getGlobalRegionScheduler().run(plugin, task -> apply.run());
        return null;
    }

    private Component aliases(ServerCommandContext context, String commandName)
    {
        PlexCommand plexCommand = plugin.getCommandHandler().getCommand(commandName);
        if (plexCommand != null)
        {
            return PlexUtils.messageComponent("commandAliases", commandName, Arrays.toString(plexCommand.getAliases().toArray(new String[0])));
        }
        Command command = plugin.getServer().getCommandMap().getCommand(commandName);
        return command == null ? PlexUtils.messageComponent("commandNotFound")
                : PlexUtils.messageComponent("commandAliases", commandName, Arrays.toString(command.getAliases().toArray(new String[0])));
    }

    private Component pagination(ServerCommandContext context)
    {
        Player player = context.player();
        if (player == null)
        {
            return PlexUtils.messageComponent("noPermissionConsole");
        }
        new MaterialDialog().open(player);
        return null;
    }

}
