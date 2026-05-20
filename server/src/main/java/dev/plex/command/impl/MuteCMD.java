package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MuteCMD extends ServerCommand
{
    public MuteCMD()
    {
        super(command("mute")
            .description("Mute a player on the server")
            .usage("/<command> <player>")
            .aliases("stfu,emute,silence,esilence")
            .permission("plex.mute")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length != 1)
        {
            return context.usage();
        }
        Player player = context.getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = context.getOfflinePlexPlayer(player.getUniqueId());

        if (punishedPlayer.isMuted())
        {
            return context.messageComponent("playerMuted");
        }

        if (context.silentCheckPermission(player, "plex.mute"))
        {
            context.send(sender, context.messageComponent("higherRankThanYou"));
            return null;
        }

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), context.getUUID(sender));
        punishment.setCustomTime(false);
        ZonedDateTime date = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
        punishment.setEndDate(date.plusSeconds(plugin.config.getInt("punishments.mute-timer", 300)));
        punishment.setType(PunishmentType.MUTE);
        punishment.setPunishedUsername(player.getName());
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason("");
        punishment.setActive(true);

        plugin.getPunishmentManager().punish(punishedPlayer, punishment);
        PlexUtils.broadcast(context.messageComponent("mutedPlayer", sender.getName(), player.getName()));
        return null;
    }

}
