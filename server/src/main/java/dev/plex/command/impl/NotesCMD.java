package dev.plex.command.impl;

import dev.plex.util.PlexUtils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.api.note.PlayerNote;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

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
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        var playerNode = playerArgument("player");
        playerNode.then(literal("list").executes(context -> executeCommand(context, commandContext ->
                findPlayer(commandContext, string(context, "player"), player -> list(commandContext, player)))));
        playerNode.then(literal("clear").executes(context -> executeCommand(context, commandContext ->
                findPlayer(commandContext, string(context, "player"), player -> clear(commandContext, player)))));
        playerNode.then(literal("add").then(greedyString("note").executes(context -> executeCommand(context, commandContext ->
                findPlayer(commandContext, string(context, "player"),
                        player -> add(commandContext, player, string(context, "note")))))));
        playerNode.then(literal("remove").then(nonNegativeInteger("id").executes(context -> executeCommand(context, commandContext ->
                findPlayer(commandContext, string(context, "player"),
                        player -> remove(commandContext, player, integer(context, "id")))))));
        command.then(playerNode);
    }

    private Component findPlayer(ServerCommandContext context, String playerName, Consumer<PlexPlayer> action)
    {
        plugin.getPlayerService().findPlayer(playerName).whenComplete((player, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to load player {0}: {1}", playerName, failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to load the player."));
            }
            else if (player == null) context.sender().sendMessage(PlexUtils.messageComponent("playerNotFound"));
            else action.accept(player);
        });
        return null;
    }

    private Component list(ServerCommandContext context, PlexPlayer player)
    {
        plugin.getNotesService().list(player.getUuid()).whenComplete((notes, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to list notes for {0}: {1}", player.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to load notes."));
                return;
            }
            if (notes.isEmpty())
            {
                context.sender().sendMessage(PlexUtils.messageComponent("noNotes"));
                return;
            }
            readNotes(context, player, notes);
        });
        return null;
    }

    private Component add(ServerCommandContext context, PlexPlayer player, String content)
    {
        Player author = context.player();
        if (author == null)
        {
            return context.usage();
        }
        String normalizedContent = String.join(" ", content.trim().split("\\s+"));
        plugin.getNotesService().add(player.getUuid(), normalizedContent, author.getUniqueId()).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to add note for {0}: {1}", player.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to add note."));
                return;
            }
            context.sender().sendMessage(PlexUtils.messageComponent("noteAdded"));
        });
        return null;
    }

    private Component remove(ServerCommandContext context, PlexPlayer player, int id)
    {
        plugin.getNotesService().remove(player.getUuid(), id).whenComplete((deleted, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to remove note {0} for {1}: {2}", id, player.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to remove note."));
                return;
            }
            context.sender().sendMessage(deleted ? PlexUtils.messageComponent("removedNote", id) : PlexUtils.messageComponent("noteNotFound"));
        });
        return null;
    }

    private Component clear(ServerCommandContext context, PlexPlayer player)
    {
        plugin.getNotesService().clear(player.getUuid()).whenComplete((count, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to clear notes for {0}: {1}", player.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to clear notes."));
                return;
            }
            context.sender().sendMessage(PlexUtils.messageComponent("clearedNotes", count));
        });
        return null;
    }

    private void readNotes(ServerCommandContext context, PlexPlayer plexPlayer, List<PlayerNote> notes)
    {
        CompletableFuture<?>[] names = notes.stream()
                .map(note -> note.author() == null
                        ? CompletableFuture.completedFuture("CONSOLE")
                        : plugin.getPlayerService().findName(note.author()).thenApply(name ->
                                name == null || name.isBlank() ? note.author().toString() : name))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(names).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to load note authors for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to load notes."));
                return;
            }
            Component noteList = PlexUtils.messageComponent("notesHeader", plexPlayer.getName());
            for (int index = 0; index < notes.size(); index++)
            {
                PlayerNote note = notes.get(index);
                Component noteLine = PlexUtils.messageComponent("notePrefix", note.id(), names[index].join(),
                        TimeUtils.useTimezone(note.timestamp()));
                noteList = noteList.append(Component.newline()).append(noteLine)
                        .append(PlexUtils.messageComponent("noteLine", note.content()));
            }
            context.sender().sendMessage(noteList);
        });
    }

}
