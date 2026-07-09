package dev.plex.menu.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class MaterialDialog
{
    private static final int PAGE_SIZE = 40;
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(1)
            .lifetime(Duration.ofMinutes(5))
            .build();

    private final List<Material> materials = Arrays.stream(Material.values())
            .filter(material -> !material.isAir())
            .toList();

    public void open(Player player)
    {
        open(player, 0);
    }

    private void open(Player player, int page)
    {
        player.showDialog(create(page));
    }

    private Dialog create(int page)
    {
        int pageCount = Math.max(1, (int) Math.ceil(materials.size() / (double) PAGE_SIZE));
        int currentPage = Math.max(0, Math.min(page, pageCount - 1));
        int fromIndex = currentPage * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, materials.size());
        List<ActionButton> actions = new ArrayList<>(materials.subList(fromIndex, toIndex).stream()
                .map(material -> materialButton(material, currentPage))
                .toList());
        if (currentPage > 0)
        {
            actions.add(pageButton("Previous Page", currentPage - 1));
        }
        if (currentPage + 1 < pageCount)
        {
            actions.add(pageButton("Next Page", currentPage + 1));
        }

        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text("Materials"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.WAIT_FOR_RESPONSE)
                        .body(List.of(DialogBody.plainMessage(Component.text("Page " + (currentPage + 1) + " of " + pageCount))))
                        .build())
                .type(DialogType.multiAction(actions, closeButton(), 4)));
    }

    private ActionButton materialButton(Material material, int page)
    {
        return ActionButton.builder(Component.text(material.name()))
                .width(120)
                .action(DialogAction.customClick((response, audience) ->
                {
                    if (audience instanceof Player player)
                    {
                        player.sendMessage(Component.text(material.name()));
                        open(player, page);
                    }
                }, CALLBACK_OPTIONS))
                .build();
    }

    private ActionButton pageButton(String label, int page)
    {
        return ActionButton.builder(Component.text(label))
                .width(150)
                .action(DialogAction.customClick((response, audience) ->
                {
                    if (audience instanceof Player player)
                    {
                        open(player, page);
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
}
