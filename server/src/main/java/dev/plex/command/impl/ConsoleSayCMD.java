package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ConsoleSayCMD extends ServerCommand
{
    public ConsoleSayCMD()
    {
        super(command("consolesay")
            .description("Displays a message to everyone")
            .usage("/<command> <message>")
            .aliases("csay")
            .permission("plex.consolesay")
            .source(RequiredCommandSource.CONSOLE)
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(greedyString("message")
                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "message")))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        PlexUtils.broadcast(PlexUtils.messageComponent("consoleSayMessage", sender.getName(), PlexUtils.mmStripColor(StringUtils.join(args, " "))));
        return null;
    }

}
