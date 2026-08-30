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
                    if (ex != null)
                    {
                        PlexLog.warn("Unable to list notes for {0}: {1}", plexPlayer.getUuid(), ex.getMessage());
                        plugin.getApi().scheduler().runGlobal(() -> context.send(sender, Component.text("Unable to load notes.")));
                        return;
                    }
                    if (notes.isEmpty())
                    {
                        plugin.getApi().scheduler().runGlobal(() -> context.send(sender, context.messageComponent("noNotes")));
                        return;
                    }
                    List<String> authors = notes.stream()
                            .map(note -> authorName(note.getWrittenBy())).toList();
                    plugin.getApi().scheduler().runGlobal(() -> readNotes(context, sender, plexPlayer, notes, authors));
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
                    Note note = new Note(plexPlayer.getUuid(), content, playerSender.getUniqueId(), ZonedDateTime.now(TimeUtils.zoneId()));
                    plugin.getNoteRepository().addNote(note).whenComplete((unused, failure) -> plugin.getApi().scheduler().runGlobal(() ->
                    {
                        if (failure != null)
                        {
                            PlexLog.warn("Unable to add note for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                            context.send(sender, Component.text("Unable to add note."));
                        }
                        else context.send(sender, context.messageComponent("noteAdded"));
                    }));
                    return null;
                }
                return context.usage();
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
                plugin.getNoteRepository().deleteNote(id, plexPlayer.getUuid()).whenComplete((deleted, ex) ->
                {
                    plugin.getApi().scheduler().runGlobal(() ->
                    {
                        if (ex != null)
                        {
                            PlexLog.warn("Unable to remove note {0} for {1}: {2}", id, plexPlayer.getUuid(), ex.getMessage());
                            context.send(sender, Component.text("Unable to remove note."));
                        }
                        else context.send(sender, deleted ? context.messageComponent("removedNote", id) : context.messageComponent("noteNotFound"));
                    });
                });
                return null;
            }
            case "clear":
            {
                plugin.getNoteRepository().clearNotes(plexPlayer.getUuid()).whenComplete((count, failure) -> plugin.getApi().scheduler().runGlobal(() ->
                {
                    if (failure != null)
                    {
                        PlexLog.warn("Unable to clear notes for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                        context.send(sender, Component.text("Unable to clear notes."));
                    }
                    else context.send(sender, context.messageComponent("clearedNotes", count));
                }));
                return null;
            }
            default:
            {
                return context.usage();
            }
        }
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
