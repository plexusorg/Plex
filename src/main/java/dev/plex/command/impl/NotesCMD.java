package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.extra.Note;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "notes", description = "Manage notes for a player", usage = "/<command> <player> <list | add <note> | remove <id> | clear>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.notes")
public class NotesCMD extends PlexCommand
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a");

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length < 2)
        {
            return usage();
        }

        Player player = getNonNullPlayer(args[0]);
        PlexPlayer plexPlayer = getPlexPlayer(player);

        switch (args[1].toLowerCase())
        {
            case "list":
            {
                plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                {
                    if (notes.size() == 0)
                    {
                        send(sender, mmString("<red>This player has no notes!"));
                        return;
                    }
                    AtomicReference<Component> noteList = new AtomicReference<>(Component.text("Player notes for: " + plexPlayer.getName()).color(NamedTextColor.GREEN));
                    for (Note note : notes)
                    {
                        Component noteLine = Component.text(note.getId() + " - Written by: " + DataUtils.getPlayer(note.getWrittenBy()).getName() + " on " + DATE_FORMAT.format(note.getTimestamp())).color(NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false);
                        noteLine = noteLine.append(Component.text(note.getNote())).color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, true);
                        noteList.set(noteList.get().append(Component.newline()));
                        noteList.set(noteList.get().append(noteLine));
                    }
                    send(sender, noteList.get());
                });
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
                    Note note = new Note(plexPlayer.getUuid(), content, playerSender.getUniqueId(), LocalDateTime.now());
                    plexPlayer.getNotes().add(note);
                    plugin.getSqlNotes().addNote(note);
                    return Component.text("Note added.").color(NamedTextColor.GREEN);
                }
            }
            case "remove":
            {
                int id;
                try
                {
                    id = Integer.parseInt(args[2]);
                }
                catch (NumberFormatException ignored)
                {
                    return Component.text("Invalid number: " + args[2]).color(NamedTextColor.RED);
                }
                plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                {
                    for (Note note : notes)
                    {
                        if (note.getId() == id)
                        {
                            plugin.getSqlNotes().deleteNote(id, plexPlayer.getUuid()).whenComplete((notes1, ex1) ->
                                    send(sender, Component.text("Removed note with ID: " + id).color(NamedTextColor.GREEN)));
                        }
                        else
                        {
                            send(sender, mmString("<red>A note with this ID could not be found"));
                        }
                    }
                });
            }
            case "clear":
            {
                plugin.getSqlNotes().getNotes(plexPlayer.getUuid()).whenComplete((notes, ex) ->
                {
                    for (Note note : notes)
                    {
                        plugin.getSqlNotes().deleteNote(note.getId(), plexPlayer.getUuid());
                    }
                    send(sender, Component.text("Cleared " + notes.size() + " note(s).").color(NamedTextColor.GREEN));
                });
                return null;
            }
            default:
            {
                return usage();
            }
        }
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