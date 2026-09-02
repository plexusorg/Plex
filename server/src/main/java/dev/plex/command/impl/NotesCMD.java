package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.extra.Note;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
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

        return switch (args[1].toLowerCase())
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
        plugin.getNoteRepository().getNotes(player.getUuid()).whenComplete((notes, failure) ->
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
            List<String> authors = notes.stream().map(note -> authorName(note.getWrittenBy())).toList();
            readNotes(context, context.sender(), player, notes, authors);
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
        Note note = new Note(player.getUuid(), content, author.getUniqueId(), ZonedDateTime.now(TimeUtils.zoneId()));
        plugin.getNoteRepository().addNote(note).whenComplete((unused, failure) ->
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
        plugin.getNoteRepository().deleteNote(id, player.getUuid()).whenComplete((deleted, failure) ->
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
        plugin.getNoteRepository().clearNotes(player.getUuid()).whenComplete((count, failure) ->
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

    private void readNotes(ServerCommandContext context, @NotNull CommandSender sender, PlexPlayer plexPlayer,
                           List<Note> notes, List<String> authors)
    {
        Component noteList = context.messageComponent("notesHeader", plexPlayer.getName());
        for (int index = 0; index < notes.size(); index++)
        {
            Note note = notes.get(index);
            Component noteLine = context.messageComponent("notePrefix", note.getId(), authors.get(index), TimeUtils.useTimezone(note.getTimestamp()));
            noteLine = noteLine.append(context.messageComponent("noteLine", note.getNote()));
            noteList = noteList.append(Component.newline()).append(noteLine);
        }
        context.send(sender, noteList);
    }

    private String authorName(UUID uuid)
    {
        if (uuid == null) return "CONSOLE";
        String name = plugin.getPlayerService().getNameByUUID(uuid);
        return name == null || name.isBlank() ? uuid.toString() : name;
    }

}
