package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.smite", source = RequiredCommandSource.ANY)
@CommandParameters(name = "smite", usage = "/<command> <player> [reason] [-ci | -q]", description = "Someone being a little bitch? Smite them down...")
public class SmiteCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length < 1)
        {
            return usage();
        }

        String reason = null;
        boolean silent = false;
        boolean clearinv = false;

        if (args.length >= 2)
        {
            if (args[args.length - 1].equalsIgnoreCase("-q"))
            {
                if (args[args.length - 1].equalsIgnoreCase("-q"))
                {
                    silent = true;
                }

                if (args.length >= 3)
                {
                    reason = StringUtils.join(ArrayUtils.subarray(args, 1, args.length - 1), " ");
                }
            }
            else if (args[args.length - 1].equalsIgnoreCase("-ci"))
            {
                if (args[args.length - 1].equalsIgnoreCase("-ci"))
                {
                    clearinv = true;
                }

                if (args.length >= 3)
                {
                    reason = StringUtils.join(ArrayUtils.subarray(args, 1, args.length - 1), " ");
                }
            }
            else
            {
                reason = StringUtils.join(ArrayUtils.subarray(args, 1, args.length), " ");
            }
        }

        final Player player = getNonNullPlayer(args[0]);
        final PlexPlayer plexPlayer = getPlexPlayer(player);

        Title title = Title.title(Component.text("You've been smitten.").color(NamedTextColor.RED), Component.text("Be sure to follow the rules!").color(NamedTextColor.YELLOW));
        player.showTitle(title);

        if (!silent)
        {
            PlexUtils.broadcast(mmString("<red>" + player.getName() + " has been a naughty, naughty boy."));
            if (reason != null)
            {
                PlexUtils.broadcast(mmString("  <red>Reason: " + "<yellow>" + reason));
            }
            PlexUtils.broadcast(mmString("  <red>Smitten by: " + "<yellow>" + sender.getName()));
        }
        else
        {
            send(sender, "Smitten " + player.getName() + " quietly.");
        }

        // Deop
        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            player.setOp(false);
        }

        // Set gamemode to survival
        player.setGameMode(GameMode.SURVIVAL);

        // Clear inventory
        if (clearinv)
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
            send(player, mmString("<red>You've been smitten. Reason: <yellow>" + reason));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (silentCheckRank(sender, Rank.ADMIN, "plex.smite") && args.length == 1)
        {
            return PlexUtils.getPlayerNameList();
        }
        return Collections.emptyList();
    }
}