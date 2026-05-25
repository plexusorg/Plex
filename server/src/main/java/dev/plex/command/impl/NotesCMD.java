package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.extra.Note;
import dev.plex.util.TimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        CommandSender sender = context.sender();
        Player playerSender = context.player();
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

        switch (args[1].toLowerCase())
        {
            case "list":
            {
                plugin.getNoteRepository().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                {
                    if (notes.isEmpty())
                    {
                        context.send(sender, context.messageComponent("noNotes"));
                        return;
                    }
                    readNotes(context, sender, plexPlayer, notes);
                });
                return null;
            }
            case "add":
            {
                if (args.length < 3)
                {
                    return context.usage();
                }
                String content = StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ");
                if (playerSender != null)
                {
                    Note note = new Note(plexPlayer.getUuid(), content, playerSender.getUniqueId(), ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE)));
                    plexPlayer.getNotes().add(note);
                    plugin.getNoteRepository().addNote(note);
                    return context.messageComponent("noteAdded");
                }
            }
            case "remove":
            {
                if (args.length < 3)
                {
                    return context.usage();
                }
                int id;
                try
                {
                    id = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException ignored)
                {
                    return context.messageComponent("unableToParseNumber", args[2]);
                }
                plugin.getNoteRepository().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                {
                    boolean deleted = false;
                    for (Note note : notes)
                    {
                        if (note.getId() == id)
                        {
                            plugin.getNoteRepository().deleteNote(id, plexPlayer.getUuid()).whenComplete((notes1, ex1) ->
                                    context.send(sender, context.messageComponent("removedNote", id)));
                            deleted = true;
                        }
                    }
                    if (!deleted)
                    {
                        context.send(sender, context.messageComponent("noteNotFound"));
                    }
                    plexPlayer.getNotes().removeIf(note -> note.getId() == id);
                });
                return null;
            }
            case "clear":
            {
                int count = plexPlayer.getNotes().size();
                plexPlayer.getNotes().clear();
                plugin.getPlayerService().update(plexPlayer);
                return context.messageComponent("clearedNotes", count);
            }
            default:
            {
                return context.usage();
            }
        }
    }

    private void readNotes(ServerCommandContext context, @NotNull CommandSender sender, PlexPlayer plexPlayer, List<Note> notes)
    {
        AtomicReference<Component> noteList = new AtomicReference<>(context.messageComponent("notesHeader", plexPlayer.getName()));
        for (Note note : notes)
        {
            String author = plugin.getPlayerNameResolver().resolve(note.getWrittenBy());
            Component noteLine = context.messageComponent("notePrefix", note.getId(), author, TimeUtils.useTimezone(note.getTimestamp()));
            noteLine = noteLine.append(context.messageComponent("noteLine", note.getNote()));
            noteList.set(noteList.get().append(Component.newline()));
            noteList.set(noteList.get().append(noteLine));
        }
        context.send(sender, noteList.get());
    }

}
