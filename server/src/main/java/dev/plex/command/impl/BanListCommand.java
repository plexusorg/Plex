package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BanListCommand extends ServerCommand
{
    public BanListCommand()
    {
        super(command("banlist")
            .description("Manages the banlist")
            .usage("/<command> [purge]")
            .permission("plex.banlist")
            .build());
    }
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
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                context.send(sender, context.messageComponent("activeBansList", punishments.size(), StringUtils.join(punishments.stream().map(punishment -> plugin.getPlayerNameResolver().resolve(punishment.getPunished())).toList(), ", ")));
            });
            return null;
        }
        if (args[0].equalsIgnoreCase("purge") || args[0].equalsIgnoreCase("clear"))
        {
            if (sender instanceof Player)
            {
                return context.messageComponent("noPermissionInGame");
            }
            if (!sender.getName().equalsIgnoreCase("console"))
            {
                if (!context.checkPermission(sender, "plex.banlist.clear"))
                {
                    return null;
                }
            }
            plugin.getPunishmentManager().getActiveBans().whenComplete((punishments, throwable) ->
            {
                punishments.forEach(plugin.getPunishmentManager()::unban);
                context.send(sender, context.messageComponent("unbannedPlayers", punishments.size()));
            });
        }
        return null;
    }

}
