package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;

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
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player")))
                .then(greedyString("reason")
                        .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-ci", "-q")))
                        .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), string(context, "reason"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length < 1)
        {
            return context.usage();
        }

        String reason = null;
        boolean silent = false;
        boolean clearInv = false;

        if (args.length >= 2)
        {
            List<String> reasonParts = new ArrayList<>();
            for (int i = 1; i < args.length; i++)
            {
                if (args[i].equalsIgnoreCase("-q"))
                {
                    silent = true;
                    continue;
                }
                if (args[i].equalsIgnoreCase("-ci"))
                {
                    clearInv = true;
                    continue;
                }
                reasonParts.add(args[i]);
            }

            if (!reasonParts.isEmpty())
            {
                reason = StringUtils.join(reasonParts, " ");
            }
        }

        final Player player = context.getNonNullPlayer(args[0]);
        final PlexPlayer plexPlayer = context.getPlexPlayer(player);

        Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
        punishment.setType(PunishmentType.SMITE);
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        String finalReason = reason != null ? reason : context.messageString("noReasonProvided");
        punishment.setReason(finalReason);
        boolean finalSilent = silent;
        boolean finalClearInv = clearInv;
        plugin.getPunishmentManager().punish(plexPlayer, punishment).whenComplete((unused, failure) ->
                plugin.getApi().scheduler().runGlobal(() ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to persist smite for {0}: {1}", player.getName(), failure.getMessage());
                context.send(sender, Component.text("Unable to persist the smite; no action was taken."));
                return;
            }
            if (!finalSilent)
            {
                PlexUtils.broadcast(context.messageComponent("smiteBroadcast", player.getName(), finalReason, context.senderName()));
            }
            else
            {
                context.send(sender, context.messageComponent("smittenQuietly", player.getName()));
            }
            plugin.getApi().scheduler().runEntity(player,
                    () -> applySmite(context, player, finalReason, finalClearInv));
        }));
        return null;
    }

    private void applySmite(ServerCommandContext context, Player player, String reason, boolean clearInventory)
    {
        Title title = Title.title(context.messageComponent("smiteTitleHeader"),
                context.messageComponent("smiteTitleMessage", reason, context.senderName()));
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
        context.send(player, context.messageComponent("smitten", reason));
    }

}
