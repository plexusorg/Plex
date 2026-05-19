package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.punishment.Punishment;

import java.util.stream.Collectors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "banlist", description = "Manages the banlist", usage = "/<command> [purge]")
@CommandPermissions(permission = "plex.banlist")
public class BanListCommand extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("purge")
                .executes(context -> executeCommand(context, "purge")));
        command.then(literal("clear")
                .executes(context -> executeCommand(context, "clear")));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args)
    {
        if (args.length == 0)
        {
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                send(sender, messageComponent("activeBansList", punishments.size(), StringUtils.join(punishments.stream().map(Punishment::getPunishedUsername).collect(Collectors.toList()), ", ")));
            });
            return null;
        }
        if (args[0].equalsIgnoreCase("purge") || args[0].equalsIgnoreCase("clear"))
        {
            if (sender instanceof Player)
            {
                return messageComponent("noPermissionInGame");
            }
            if (!sender.getName().equalsIgnoreCase("console"))
            {
                if (!checkPermission(sender, "plex.banlist.clear"))
                {
                    return null;
                }
            }
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                punishments.forEach(plugin.getPunishmentManager()::unban);
                send(sender, messageComponent("unbannedPlayers", punishments.size()));
            });
        }
        return null;
    }

}
