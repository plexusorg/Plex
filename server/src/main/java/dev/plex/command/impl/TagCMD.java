package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TagCMD extends ServerCommand
{
    public TagCMD()
    {
        super(command("tag")
            .description("Set or clear your prefix")
            .usage("/<command> <set <prefix> | clear <player>>")
            .aliases("prefix")
            .permission("plex.tag")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("set")
                .executes(context -> executeCommand(context, "set"))
                .then(greedyString("prefix")
                        .executes(context -> executeCommand(context, argsWithGreedy("set", string(context, "prefix"))))));
        command.then(literal("clear")
                .executes(context -> executeCommand(context, "clear"))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context, "clear", string(context, "player")))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            if (sender instanceof ConsoleCommandSender)
            {
                return context.usage("/tag clear <player>");
            }
            return context.usage();
        }

        if (args[0].equalsIgnoreCase("set"))
        {
            if (sender instanceof ConsoleCommandSender)
            {
                return context.messageComponent("noPermissionConsole");
            }
            assert playerSender != null;
            PlexPlayer player = plugin.getPlayerService().getPlayer(playerSender.getUniqueId());
            if (args.length < 2)
            {
                return context.usage("/tag set <prefix>");
            }

            Component convertedComponent = PlexUtils.stringToComponent(StringUtils.join(args, " ", 1, args.length));

            if (PlainTextComponentSerializer.plainText().serialize(convertedComponent).length() > plugin.config.getInt("chat.max-tag-length", 16))
            {
                return context.messageComponent("maximumPrefixLength", plugin.config.getInt("chat.max-tag-length", 16));
            }

            player.setPrefix(MiniMessage.miniMessage().serialize(convertedComponent));
            plugin.getPlayerService().update(player);
            return context.messageComponent("prefixSetTo", MiniMessage.miniMessage().serialize(convertedComponent));
        }

        if (args[0].equalsIgnoreCase("clear"))
        {
            if (args.length == 1)
            {
                if (sender instanceof ConsoleCommandSender)
                {
                    return context.messageComponent("noPermissionConsole");
                }

                if (playerSender == null)
                {
                    return null;
                }

                PlexPlayer player = plugin.getPlayerService().getPlayer(playerSender.getUniqueId());
                player.setPrefix(null);
                plugin.getPlayerService().update(player);
                return context.messageComponent("prefixCleared");
            }
            context.checkPermission(sender, "plex.tag.clear.others");
            Player target = context.getNonNullPlayer(args[1]);
            PlexPlayer plexTarget = plugin.getPlayerService().getPlayer(target.getUniqueId());
            plexTarget.setPrefix(null);
            plugin.getPlayerService().update(plexTarget);
            return context.messageComponent("otherPrefixCleared", target.getName());
        }
        return context.usage();
    }

}


