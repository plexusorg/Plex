package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.ToggleMenu;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandParameters(name = "toggle", description = "Allows toggling various server aspects through a GUI", aliases = "toggles")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.toggle", source = RequiredCommandSource.ANY)
public class ToggleCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (isConsole(sender) || playerSender == null)
        {
            if (args.length == 0)
            {
                sender.sendMessage(PlexUtils.mmDeserialize("<gray>Available toggles:"));
                sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Explosions" + status("explosions")));
                sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Fluidspread" + status("fluidspread")));
                sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Drops" + status("drops")));
                sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Redstone" + status("redstone")));
                return null;
            }
            switch (args[0].toLowerCase())
            {
                case "explosions" ->
                {
                    return toggle("explosions");
                }
                case "fluidspread" ->
                {
                    return toggle("fluidspread");
                }
                case "drops" ->
                {
                    return toggle("drops");
                }
                case "redstone" ->
                {
                    return toggle("redstone");
                }
                default ->
                {
                    return messageComponent("invalidToggle");
                }
            }
        }
        new ToggleMenu().openInv(playerSender, 0);
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }

    private String status(String toggle)
    {
        return plugin.toggles.getBoolean(toggle) ? " (enabled)" : " (disabled)";
    }

    private Component toggle(String toggle)
    {
        plugin.toggles.set(toggle, !plugin.getToggles().getBoolean(toggle));
        return Component.text("Toggled " + toggle + status(toggle)).color(NamedTextColor.GRAY);
    }
}
