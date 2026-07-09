package dev.plex.menu.dialog;

import dev.plex.Plex;
import dev.plex.util.PlexUtils;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

public class ToggleDialog
{
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private final Plex plugin;

    public ToggleDialog(Plex plugin)
    {
        this.plugin = plugin;
    }

    public void open(Player player)
    {
        player.showDialog(create());
    }

    private Dialog create()
    {
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(PlexUtils.messageComponent("toggleMenuTitle"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(List.of(
                                DialogBody.plainMessage(statusLine("toggleExplosions", "explosions", true)),
                                DialogBody.plainMessage(statusLine("toggleFluidSpread", "fluidspread", false)),
                                DialogBody.plainMessage(statusLine("toggleDrops", "drops", false)),
                                DialogBody.plainMessage(statusLine("toggleRedstone", "redstone", false)),
                                DialogBody.plainMessage(statusLine("togglePvp", "pvp", false)),
                                DialogBody.plainMessage(statusLine("toggleChat", PlexUtils.messageComponent(plugin.toggles.getBoolean("chat") ? "stateOn" : "stateOff")))))
                        .build())
                .type(DialogType.multiAction(List.of(
                                toggleButton("toggleExplosions", "toggleExplosionsLower", "explosions"),
                                toggleButton("toggleFluidSpread", "toggleFluidSpreadLower", "fluidspread"),
                                toggleButton("toggleDrops", "toggleDropsLower", "drops"),
                                toggleButton("toggleRedstone", "toggleRedstoneLower", "redstone"),
                                toggleButton("togglePvp", "togglePvpLower", "pvp"),
                                toggleButton("toggleChat", "toggleChatLower", "chat")),
                        closeButton(), 2)));
    }

    private ActionButton toggleButton(String nameKey, String lowerNameKey, String toggle)
    {
        return ActionButton.builder(PlexUtils.messageComponent(nameKey))
                .tooltip(PlexUtils.messageComponent(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled"))
                .width(150)
                .action(DialogAction.customClick((response, audience) -> toggle(audience, lowerNameKey, toggle), CALLBACK_OPTIONS))
                .build();
    }

    private ActionButton closeButton()
    {
        return ActionButton.builder(Component.text("Close"))
                .width(150)
                .build();
    }

    private void toggle(Audience audience, String lowerNameKey, String toggle)
    {
        if (!(audience instanceof Player player) || !player.hasPermission("plex.toggle"))
        {
            return;
        }

        plugin.toggles.set(toggle, !plugin.toggles.getBoolean(toggle));
        if ("chat".equals(toggle))
        {
            PlexUtils.broadcast(PlexUtils.messageComponent("chatToggled", player.getName(), PlexUtils.messageString(plugin.toggles.getBoolean("chat") ? "stateOn" : "stateOff")));
        }
        player.sendMessage(PlexUtils.messageComponent("toggleToggled", PlexUtils.messageString(lowerNameKey)));
        open(player);
    }

    private Component statusLine(String nameKey, String toggle, boolean enabledIsUnsafe)
    {
        return statusLine(nameKey, status(toggle, enabledIsUnsafe));
    }

    private Component statusLine(String nameKey, Component status)
    {
        return PlexUtils.messageComponent(nameKey)
                .append(Component.text(": "))
                .append(status);
    }

    private Component status(String toggle, boolean enabledIsUnsafe)
    {
        if (enabledIsUnsafe)
        {
            return PlexUtils.messageComponent(plugin.toggles.getBoolean(toggle) ? "stateEnabledUnsafe" : "stateDisabledSafe");
        }
        return PlexUtils.messageComponent(plugin.toggles.getBoolean(toggle) ? "stateEnabled" : "stateDisabled");
    }
}
