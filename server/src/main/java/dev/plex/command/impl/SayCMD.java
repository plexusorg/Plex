package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SayCMD extends ServerCommand
{
    public SayCMD()
    {
        super(command("say")
            .description("Displays a message to everyone")
            .usage("/<command> <message>")
            .permission("plex.say")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(greedyString("message")
                .executes(context -> executeCommand(context, commandContext -> say(commandContext,
                        normalizeGreedyString(string(context, "message"))))));
    }

    private Component say(ServerCommandContext context, String message)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        PlexUtils.broadcast(PlexUtils.messageComponent("sayMessage", context.senderName(), PlexUtils.mmStripColor(message)));
        return null;
    }

}
