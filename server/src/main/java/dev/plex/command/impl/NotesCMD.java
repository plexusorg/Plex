package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.note.PlayerNote;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.util.List;
import java.util.UUID;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class NotesCMD extends ServerCommand
{
    public NotesCMD()
    {
        super(command("notes")
            .description("Manage notes for a player")
            .usage("/<command> <player> <list | add <note> | remove <id> | clear>")
            .permission("plex.notes")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .then(literal("list")
                        .executes(context -> executeCommand(context, string(context, "player"), "list")))
                .then(literal("clear")
                        .executes(context -> executeCommand(context, string(context, "player"), "clear")))
                .then(literal("add")
                        .then(greedyString("note")
                                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), "add", string(context, "note"))))))
                .then(literal("remove")
                        .then(nonNegativeInteger("id")
                                .executes(context -> executeCommand(context, string(context, "player"), "remove", String.valueOf(integer(context, "id")))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        String[] args = context.args();
        if (args.length < 2)
        {
            return context.usage();
        }

        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[0]);

        if (plexPlayer == null)
        {
            return context.messageComponent("playerNotFound");
        }

        return switch (args[1])
        {
            case "list" -> list(context, plexPlayer);
            case "add" -> add(context, plexPlayer, args);
            case "remove" -> remove(context, plexPlayer, args);
            case "clear" -> clear(context, plexPlayer);
            default -> context.usage();
        };
    }

    private Component list(ServerCommandContext context, PlexPlayer player)
    {
        plugin.getApi().notes().list(player.getUuid()).whenComplete((notes, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to list notes for {0}: {1}", player.getUuid(), failure.getMessage());
                context.send(context.sender(), Component.text("Unable to load notes."));
                return;
            }
            if (notes.isEmpty())
            {
                context.send(context.sender(), context.messageComponent("noNotes"));
                return;
            }
            readNotes(context, player, notes);
        });
        return null;
    }

    private Component add(ServerCommandContext context, PlexPlayer player, String[] args)
    {
        Player author = context.player();
        if (args.length < 3 || author == null)
        {
            return context.usage();
        }
        String content = StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ");
        plugin.getApi().notes().add(player.getUuid(), content, author.getUniqueId()).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to add note for {0}: {1}", player.getUuid(), failure.getMessage());
                context.send(context.sender(), Component.text("Unable to add note."));
                return;
            }
            context.send(context.sender(), context.messageComponent("noteAdded"));
        });
        return null;
    }

    private Component remove(ServerCommandContext context, PlexPlayer player, String[] args)
    {
        if (args.length < 3)
        {
            return context.usage();
        }
        int id = Integer.parseInt(args[2]);
        plugin.getApi().notes().remove(player.getUuid(), id).whenComplete((deleted, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to remove note {0} for {1}: {2}", id, player.getUuid(), failure.getMessage());
                context.send(context.sender(), Component.text("Unable to remove note."));
                return;
            }
            context.send(context.sender(), deleted ? context.messageComponent("removedNote", id) : context.messageComponent("noteNotFound"));
        });
        return null;
    }

    private Component clear(ServerCommandContext context, PlexPlayer player)
    {
        plugin.getApi().notes().clear(player.getUuid()).whenComplete((count, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to clear notes for {0}: {1}", player.getUuid(), failure.getMessage());
                context.send(context.sender(), Component.text("Unable to clear notes."));
                return;
            }
            context.send(context.sender(), context.messageComponent("clearedNotes", count));
        });
        return null;
    }

    private void readNotes(ServerCommandContext context, PlexPlayer plexPlayer, List<PlayerNote> notes)
    {
        Component noteList = context.messageComponent("notesHeader", plexPlayer.getName());
        for (PlayerNote note : notes)
        {
            Component noteLine = context.messageComponent("notePrefix", note.id(), authorName(note.author()), TimeUtils.useTimezone(note.timestamp()));
            noteLine = noteLine.append(context.messageComponent("noteLine", note.content()));
            noteList = noteList.append(Component.newline()).append(noteLine);
        }
        context.send(context.sender(), noteList);
    }

    private String authorName(UUID uuid)
    {
        if (uuid == null) return "CONSOLE";
        String name = plugin.getPlayerService().getNameByUUID(uuid);
        return name == null || name.isBlank() ? uuid.toString() : name;
    }

}
