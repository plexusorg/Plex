package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.annotation.System;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.extra.Note;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
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
                Component noteList = Component.text("Player notes for: " + plexPlayer.getName()).color(NamedTextColor.GREEN);
                int id = 1;
                for (Note note : plexPlayer.getNotes())
                {
                    Component noteLine = Component.text(id + ". " + note.getWrittenBy() + ": " + note.getNote()).color(NamedTextColor.GOLD);
                    noteList.append(Component.empty()).append(noteLine);
                    id++;
                }
                send(sender, noteList);
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
                    Note note = new Note(UUID.fromString(plexPlayer.getUuid()), content, playerSender.getUniqueId(), LocalDateTime.now());
                    plexPlayer.getNotes().add(note);
                    plugin.getSqlNotes().addNote(note);
                    return Component.text("Note added.").color(NamedTextColor.GREEN);
                }
            }
            case "remove":
            {
                return null;
            }
            case "clear":
            {
                int count = plexPlayer.getNotes().size();
                final List<Note> notes = plexPlayer.getNotes();
                for (Note note : notes)
                {
                    plexPlayer.getNotes().remove(note);
                    count++;
                }
                DataUtils.update(plexPlayer);
                return Component.text("Cleared " + count + " note(s).").color(NamedTextColor.GREEN);
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