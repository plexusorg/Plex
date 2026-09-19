package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.player.TagTooLongException;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        String normalizedPrefix = String.join(" ", prefix.trim().split("\\s+"));
        Component tag = PlexUtils.stringToComponent(normalizedPrefix);
        report(context.sender(), plugin.getApi().players().setTag(playerSender.getUniqueId(), tag),
                PlexUtils.messageComponent("prefixSetTo", Placeholder.component("prefix", tag)));
        return null;
    }

    private Component clearOwn(ServerCommandContext context)
    {
        Player playerSender = context.player();
        if (playerSender == null)
        {
            return PlexUtils.messageComponent("noPermissionConsole");
        }
        report(context.sender(), plugin.getApi().players().clearTag(playerSender.getUniqueId()),
                PlexUtils.messageComponent("prefixCleared"));
        return null;
    }

    private Component clearOther(ServerCommandContext context, String playerName)
    {
        context.checkPermission(context.sender(), "plex.tag.clear.others");
        Player target = getNonNullPlayer(playerName);
        report(context.sender(), plugin.getApi().players().clearTag(target.getUniqueId()),
                PlexUtils.messageComponent("otherPrefixCleared", Placeholder.unparsed("player", target.getName())));
        return null;
    }

    private void report(CommandSender sender, CompletableFuture<Void> update, Component success)
    {
        update.whenComplete((ignored, failure) ->
        {
            if (failure == null)
            {
                sender.sendMessage(success);
                return;
            }
            Throwable cause = failure instanceof CompletionException ? failure.getCause() : failure;
            if (cause instanceof TagTooLongException tooLong)
            {
                sender.sendMessage(PlexUtils.messageComponent("maximumPrefixLength",
                        Placeholder.unparsed("max_length", String.valueOf(tooLong.maximumLength()))));
                return;
            }
            if (cause instanceof IllegalArgumentException)
            {
                sender.sendMessage(Component.text(cause.getMessage(), NamedTextColor.RED));
                return;
            }
            PlexLog.error("Unable to save a player tag", cause);
            sender.sendMessage(Component.text("Could not save the tag. Check the server logs.", NamedTextColor.RED));
        });
    }
}
