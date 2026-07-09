package dev.plex.menu.dialog;

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
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PunishmentDialog
{
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private final PlayerService playerService;

    public PunishmentDialog(PlayerService playerService)
    {
        this.playerService = playerService;
    }

    public void open(Player player)
    {
        player.showDialog(playerListDialog());
    }

    public void open(Player viewer, PlexPlayer punishedPlayer)
    {
        viewer.showDialog(playerPunishmentsDialog(punishedPlayer));
    }

    private Dialog playerListDialog()
    {
        List<ActionButton> actions = Bukkit.getOnlinePlayers().stream()
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

    private Dialog playerPunishmentsDialog(PlexPlayer punishedPlayer)
    {
        List<DialogBody> body = new ArrayList<>();
        List<Punishment> punishments = punishedPlayer.getPunishments();

        if (punishments.isEmpty())
        {
            body.add(DialogBody.plainMessage(Component.text("No punishments found.")));
        }
        else
        {
            punishments.forEach(punishment -> body.add(DialogBody.plainMessage(punishmentSummary(punishment), 320)));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(PlexUtils.messageComponent("punishedPlayerMenuTitle", punishedPlayer.getName()))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(body)
                        .build())
                .type(DialogType.multiAction(List.of(backButton()), closeButton(), 2)));
    }

    private ActionButton playerButton(Player player)
    {
        return ActionButton.builder(Component.text(player.getName()))
                .width(150)
                .action(DialogAction.customClick((response, audience) -> openPunishments(audience, player), CALLBACK_OPTIONS))
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

    private void openPunishments(Audience audience, OfflinePlayer selectedPlayer)
    {
        if (!(audience instanceof Player viewer))
        {
            return;
        }

        PlexPlayer punishedPlayer = playerService.getPlayer(selectedPlayer.getUniqueId());
        if (punishedPlayer == null)
        {
            viewer.sendMessage(PlexUtils.messageComponent("punishmentPlayerNotFound"));
            return;
        }
        open(viewer, punishedPlayer);
    }

    private Component punishmentSummary(Punishment punishment)
    {
        String punisher = punishment.getPunisher() == null ? "CONSOLE" : playerService.getNameByUUID(punishment.getPunisher());
        return Component.text(punishment.getType().name() + "\n"
                + "By: " + punisher + "\n"
                + "Issued: " + TimeUtils.useTimezone(punishment.getIssueDate()) + "\n"
                + "Expire(d/s): " + TimeUtils.useTimezone(punishment.getEndDate()) + "\n"
                + "Reason: " + punishment.getReason());
    }
}
