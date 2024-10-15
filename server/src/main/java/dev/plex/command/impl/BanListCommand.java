package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.punishment.Punishment;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "banlist", description = "Manages the banlist", usage = "/<command> [purge]")
@CommandPermissions(permission = "plex.banlist")
public class BanListCommand extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args)
    {
        if (args.length == 0)
        {
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                send(sender, mmString("<gold>Active Bans (" + punishments.size() + "): <yellow>" + StringUtils.join(punishments.stream().map(Punishment::getPunishedUsername).collect(Collectors.toList()), ", ")));
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
                send(sender, mmString("<gold>Unbanned " + punishments.size() + " players."));
            });
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, "plex.banlist.clear") ? List.of("purge", "clear") : ImmutableList.of();
    }
}
