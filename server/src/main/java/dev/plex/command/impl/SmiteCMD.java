package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.smite", source = RequiredCommandSource.ANY)
@CommandParameters(name = "smite", usage = "/<command> <player> [reason] [-ci | -q]", description = "Someone being a little bitch? Smite them down...")
public class SmiteCMD extends ServerCommand
{
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
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length < 1)
        {
            return usage();
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

        final Player player = getNonNullPlayer(args[0]);
        final PlexPlayer plexPlayer = getPlexPlayer(player);

        Title title = Title.title(messageComponent("smiteTitleHeader"), messageComponent("smiteTitleMessage", reason, sender.getName()));
        player.showTitle(title);

        if (!silent)
        {
            PlexUtils.broadcast(messageComponent("smiteBroadcast", player.getName(), reason != null ? reason : messageString("noReasonProvided"), sender.getName()));
        }
        else
        {
            send(sender, messageComponent("smittenQuietly", player.getName()));
        }

        // Set gamemode to survival
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory
        if (clearInv)
        {
            player.getInventory().clear();
        }

        // Strike with lightning effect
        final Location targetPos = player.getLocation();
        final World world = player.getWorld();
        for (int x = -1; x <= 1; x++)
        {
            for (int z = -1; z <= 1; z++)
            {
                final Location strike_pos = new Location(world, targetPos.getBlockX() + x, targetPos.getBlockY(), targetPos.getBlockZ() + z);
                world.strikeLightning(strike_pos);
            }
        }

        // Kill
        player.setHealth(0.0);

        Punishment punishment = new Punishment(plexPlayer.getUuid(), getUUID(sender));
        punishment.setCustomTime(false);
        punishment.setEndDate(ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE)));
        punishment.setType(PunishmentType.SMITE);
        punishment.setPunishedUsername(player.getName());
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());

        if (reason != null)
        {
            punishment.setReason(reason);
        }
        send(player, messageComponent("smitten", reason != null ? reason : messageString("noReasonProvided")));
        return null;
    }

}
