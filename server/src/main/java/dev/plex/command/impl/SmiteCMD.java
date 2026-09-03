package dev.plex.command.impl;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.BanKickUtil;

import java.util.ArrayList;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SmiteCMD extends ServerCommand
{
    public SmiteCMD()
    {
        super(command("smite")
            .description("Someone being a little bitch? Smite them down...")
            .usage("/<command> <player> [reason] [-ci | -q]")
            .permission("plex.smite")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext ->
                        smite(commandContext, string(context, "player"), SmiteOptions.EMPTY)))
                .then(greedyString("reason")
                        .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-ci", "-q")))
                        .executes(context -> executeCommand(context, commandContext -> smite(commandContext,
                                string(context, "player"), parseOptions(normalizeGreedyString(string(context, "reason"))))))));
    }

    private SmiteOptions parseOptions(String tail)
    {
        List<String> reasonParts = new ArrayList<>();
        boolean silent = false;
        boolean clearInventory = false;
        for (String token : tail.split(" "))
        {
            if (token.equalsIgnoreCase("-q")) silent = true;
            else if (token.equalsIgnoreCase("-ci")) clearInventory = true;
            else reasonParts.add(token);
        }
        return new SmiteOptions(reasonParts.isEmpty() ? null : String.join(" ", reasonParts), silent, clearInventory);
    }

    private Component smite(ServerCommandContext context, String playerName, SmiteOptions options)
    {
        CommandSender sender = context.sender();
        final Player player = getNonNullPlayer(playerName);
        final PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(player.getUniqueId());

        Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
        punishment.setType(PunishmentType.SMITE);
        String finalReason = options.reason() != null ? options.reason() : PlexUtils.messageString("noReasonProvided");
        punishment.setReason(finalReason);
        BanKickUtil.currentOrLastIp(plugin, plexPlayer).thenCompose(ip ->
        {
            punishment.setIp(ip);
            return plugin.getPunishmentManager().punish(plexPlayer, punishment);
        }).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to persist smite for {0}: {1}", player.getName(), failure.getMessage());
                sender.sendMessage(Component.text("Unable to persist the smite; no action was taken."));
                return;
            }
            if (!options.silent())
            {
                PlexUtils.broadcast(PlexUtils.messageComponent("smiteBroadcast", player.getName(), finalReason, context.senderName()));
            }
            else
            {
                sender.sendMessage(PlexUtils.messageComponent("smittenQuietly", player.getName()));
            }
            player.getScheduler().run(plugin,
                    task -> applySmite(context, player, finalReason, options.clearInventory()), null);
        });
        return null;
    }

    private void applySmite(ServerCommandContext context, Player player, String reason, boolean clearInventory)
    {
        Title title = Title.title(PlexUtils.messageComponent("smiteTitleHeader"),
                PlexUtils.messageComponent("smiteTitleMessage", reason, context.senderName()));
        player.showTitle(title);
        player.setGameMode(GameMode.SURVIVAL);
        if (clearInventory) player.getInventory().clear();

        Location target = player.getLocation();
        World world = player.getWorld();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                world.strikeLightning(new Location(world, target.getBlockX() + x, target.getBlockY(), target.getBlockZ() + z));
            }
        }
        player.setHealth(0.0);
        player.sendMessage(PlexUtils.messageComponent("smitten", reason));
    }

    private record SmiteOptions(String reason, boolean silent, boolean clearInventory)
    {
        private static final SmiteOptions EMPTY = new SmiteOptions(null, false, false);
    }

}
