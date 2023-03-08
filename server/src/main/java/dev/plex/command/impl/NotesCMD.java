package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.extra.Note;
import dev.plex.rank.enums.Rank;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@CommandParameters(name = "notes", description = "Manage notes for a player", usage = "/<command> <player> <list | add <note> | remove <id> | clear>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.notes")
public class NotesCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length < 2)
        {
            return usage();
        }

        PlexPlayer plexPlayer = DataUtils.getPlayer(args[0]);

        if (plexPlayer == null)
        {
            return messageComponent("playerNotFound");
        }

        switch (args[1].toLowerCase())
        {
            case "list":
            {
                if (plugin.getStorageType() != StorageType.MONGODB)
                {
                    plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                    {
                        if (notes.size() == 0)
                        {
                            send(sender, messageComponent("noNotes"));
                            return;
                        }
                        readNotes(sender, plexPlayer, notes);
                    });
                }
                else
                {
                    List<Note> notes = plexPlayer.getNotes();
                    if (notes.size() == 0)
                    {
                        return messageComponent("noNotes");
                    }
                    readNotes(sender, plexPlayer, notes);
                }
                return null;
            }
            case "add":
            {
                if (args.length < 3)
                {
                    return usage();
                }
                String content = StringUtils.join(ArrayUtils.subarray(args, 2, args.length), " ");
                if (playerSender != null)
                {
                    Note note = new Note(plexPlayer.getUuid(), content, playerSender.getUniqueId(), ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE)));
                    plexPlayer.getNotes().add(note);
                    if (plugin.getStorageType() != StorageType.MONGODB)
                    {
                        plugin.getSqlNotes().addNote(note);
                    }
                    else
                    {
                        DataUtils.update(plexPlayer);
                    }
                    return messageComponent("noteAdded");
                }
            }
            case "remove":
            {
                if (args.length < 3)
                {
                    return usage();
                }
                int id;
                try
                {
                    id = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException ignored)
                {
                    return messageComponent("unableToParseNumber", args[2]);
                }
                if (plugin.getStorageType() != StorageType.MONGODB)
                {
                    plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                    {
                        boolean deleted = false;
                        for (Note note : notes)
                        {
                            if (note.getId() == id)
                            {
                                plugin.getSqlNotes().deleteNote(id, plexPlayer.getUuid()).whenComplete((notes1, ex1) ->
                                        send(sender, messageComponent("removedNote", id)));
                                deleted = true;
                            }
                        }
                        if (!deleted)
                        {
                            send(sender, messageComponent("noteNotFound"));
                        }
                        plexPlayer.getNotes().removeIf(note -> note.getId() == id);
                    });
                }
                else
                {
                    if (plexPlayer.getNotes().removeIf(note -> note.getId() == id))
                    {
                        return messageComponent("removedNote", id);
                    }
                    return messageComponent("noteNotFound");
                }
                return null;
            }
            case "clear":
            {
                if (plugin.getStorageType() != StorageType.MONGODB)
                {
                    plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                    {
                        for (Note note : notes)
                        {
                            plugin.getSqlNotes().deleteNote(note.getId(), plexPlayer.getUuid());
                        }
                        plexPlayer.getNotes().clear();
                        send(sender, messageComponent("clearedNotes", notes.size()));
                    });
                }
                else
                {
                    int count = plexPlayer.getNotes().size();
                    plexPlayer.getNotes().clear();
                    DataUtils.update(plexPlayer);
                    return messageComponent("clearedNotes", count);
                }
                return null;
            }
            default:
            {
                return usage();
            }
        }
    }

    private void readNotes(@NotNull CommandSender sender, PlexPlayer plexPlayer, List<Note> notes)
    {
        AtomicReference<Component> noteList = new AtomicReference<>(Component.text("Player notes for: " + plexPlayer.getName()).color(NamedTextColor.GREEN));
        for (Note note : notes)
        {
            Component noteLine = mmString("<gold><!italic>" + note.getId() + " - Written by: " + DataUtils.getPlayer(note.getWrittenBy()).getName() + " on " + TimeUtils.useTimezone(note.getTimestamp()));
            noteLine = noteLine.append(mmString("<newline><yellow># " + note.getNote()));
            noteList.set(noteList.get().append(Component.newline()));
            noteList.set(noteList.get().append(noteLine));
        }
        send(sender, noteList.get());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return PlexUtils.getPlayerNameList();
        }
        if (args.length == 2)
        {
            return Arrays.asList("list", "add", "remove", "clear");
        }
        return Collections.emptyList();
    }
}