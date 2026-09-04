package dev.plex.menu.dialog;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import dev.plex.Plex;
import dev.plex.player.PlayerService;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

public class PunishmentDialog
{
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private final PlayerService playerService;
    private final Plex plugin;

    public PunishmentDialog(Plex plugin, PlayerService playerService)
    {
        this.plugin = plugin;
        this.playerService = playerService;
    }

    public void open(Player player)
    {
        player.showDialog(playerListDialog());
    }

    public void open(Player viewer, PlexPlayer punishedPlayer)
    {
        Map<UUID, CompletableFuture<String>> names = punishedPlayer.getPunishments().stream()
                .map(Punishment::getPunisher)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), playerService::findName));
        CompletableFuture.allOf(names.values().toArray(CompletableFuture[]::new)).whenComplete((unused, failure) ->
                viewer.getScheduler().run(plugin, task ->
                {
                    if (failure != null)
                    {
                        viewer.sendMessage(Component.text("Unable to load punishment details."));
                        return;
                    }
                    viewer.showDialog(playerPunishmentsDialog(punishedPlayer, names));
                }, null));
    }

    private Dialog playerListDialog()
    {
        List<ActionButton> actions = playerService.cachedPlayers().stream()
                .map(this::playerButton)
                .toList();

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(PlexUtils.messageComponent("punishmentMenuTitle"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(List.of(DialogBody.plainMessage(Component.text("Select an online player to view their punishments."))))
                        .build())
                .type(DialogType.multiAction(actions, closeButton(), 2)));
    }

    private Dialog playerPunishmentsDialog(PlexPlayer punishedPlayer, Map<UUID, CompletableFuture<String>> names)
    {
        List<DialogBody> body = new ArrayList<>();
        List<Punishment> punishments = punishedPlayer.getPunishments();

        if (punishments.isEmpty())
        {
            body.add(DialogBody.plainMessage(Component.text("No punishments found.")));
        }
        else
        {
            punishments.forEach(punishment -> body.add(DialogBody.plainMessage(punishmentSummary(punishment, names), 320)));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(PlexUtils.messageComponent("punishedPlayerMenuTitle", placeholder("player", punishedPlayer.getName())))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(List.of(backButton()), closeButton(), 2)));
    }

    private ActionButton playerButton(PlexPlayer player)
    {
        return ActionButton.builder(Component.text(player.getName()))
                .width(150)
                .action(DialogAction.customClick((response, audience) -> openPunishments(audience, player.getUuid()), CALLBACK_OPTIONS))
                .build();
    }

    private ActionButton backButton()
    {
        return ActionButton.builder(Component.text("Back"))
                .width(150)
                .action(DialogAction.customClick((response, audience) ->
                {
                    if (audience instanceof Player player)
                    {
                        open(player);
                    }
                }, CALLBACK_OPTIONS))
                .build();
    }

    private ActionButton closeButton()
    {
        return ActionButton.builder(Component.text("Close"))
                .width(150)
                .build();
    }

    private void openPunishments(Audience audience, UUID selectedPlayer)
    {
        if (!(audience instanceof Player viewer))
        {
            return;
        }

        playerService.findPlayer(selectedPlayer).whenComplete((punishedPlayer, failure) ->
                viewer.getScheduler().run(plugin, task ->
                {
                    if (failure != null)
                    {
                        viewer.sendMessage(Component.text("Unable to load the player's punishments."));
                    }
                    else if (punishedPlayer == null)
                    {
                        viewer.sendMessage(PlexUtils.messageComponent("punishmentPlayerNotFound"));
                    }
                    else
                    {
                        open(viewer, punishedPlayer);
                    }
                }, null));
    }

    private Component punishmentSummary(Punishment punishment, Map<UUID, CompletableFuture<String>> names)
    {
        String punisher = punishment.getPunisher() == null ? "CONSOLE" : names.get(punishment.getPunisher()).join();
        return Component.text(punishment.getType().name() + "\n"
                + "By: " + punisher + "\n"
                + "Issued: " + TimeUtils.useTimezone(punishment.getIssueDate()) + "\n"
                + "Expire(d/s): " + (punishment.getEndDate() == null
                ? "N/A" : TimeUtils.useTimezone(punishment.getEndDate())) + "\n"
                + "Reason: " + punishment.getReason());
    }
}
