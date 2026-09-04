package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;


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
        command.executes(context -> executeCommand(context, this::usage));
        command.then(literal("set")
                .executes(context -> executeCommand(context,
                        commandContext -> commandContext.usage("/tag set <prefix>")))
                .then(greedyString("prefix")
                        .executes(context -> executeCommand(context,
                                commandContext -> set(commandContext, string(context, "prefix"))))));
        command.then(literal("clear")
                .executes(context -> executeCommand(context, this::clearOwn))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context,
                                commandContext -> clearOther(commandContext, string(context, "player"))))));
    }

    private Component usage(ServerCommandContext context)
    {
        return context.player() == null ? context.usage("/tag clear <player>") : context.usage();
    }

    private Component set(ServerCommandContext context, String prefix)
    {
        Player playerSender = context.player();
        if (playerSender == null)
        {
            return PlexUtils.messageComponent("noPermissionConsole");
        }
        PlexPlayer player = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
        String normalizedPrefix = String.join(" ", prefix.trim().split("\\s+"));
        Component convertedComponent = PlexUtils.stringToComponent(normalizedPrefix);

            if (PlainTextComponentSerializer.plainText().serialize(convertedComponent).length() > plugin.config.getInt("chat.max-tag-length", 16))
            {
                return PlexUtils.messageComponent("maximumPrefixLength", placeholder("max_length", plugin.config.getInt("chat.max-tag-length", 16)));
            }

        player.setPrefix(MiniMessage.miniMessage().serialize(convertedComponent));
        plugin.getPlayerService().update(player);
        return PlexUtils.messageComponent("prefixSetTo", placeholder("prefix", MiniMessage.miniMessage().serialize(convertedComponent)));
    }

    private Component clearOwn(ServerCommandContext context)
    {
        Player playerSender = context.player();
        if (playerSender == null)
        {
            return PlexUtils.messageComponent("noPermissionConsole");
        }
        PlexPlayer player = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
        player.setPrefix(null);
        plugin.getPlayerService().update(player);
        return PlexUtils.messageComponent("prefixCleared");
    }

    private Component clearOther(ServerCommandContext context, String playerName)
    {
        context.checkPermission(context.sender(), "plex.tag.clear.others");
        Player target = getNonNullPlayer(playerName);
        PlexPlayer plexTarget = plugin.getPlayerService().cachedPlayer(target.getUniqueId());
        plexTarget.setPrefix(null);
        plugin.getPlayerService().update(plexTarget);
        return PlexUtils.messageComponent("otherPrefixCleared", placeholder("player", target.getName()));
    }

}
