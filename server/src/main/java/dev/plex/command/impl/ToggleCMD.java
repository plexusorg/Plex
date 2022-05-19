package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.menu.ToggleMenu;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "toggle", usage = "/<command>", description = "Allows toggling various server aspects through a GUI")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.toggle", source = RequiredCommandSource.ANY)
public class ToggleCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (isConsole(sender) || playerSender == null)
        {
            sender.sendMessage(PlexUtils.mmDeserialize("<gray>Available toggles:"));
            sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Explosions " + status("explosions")));
            sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Fluidspread " + status("fluidspread")));
            sender.sendMessage(PlexUtils.mmDeserialize("<gray>  - Drops " + status("drops")));
            switch (args[0].toLowerCase())
            {
                case "explosions":
                {
                    toggle("explosions");
                }
                case "fluidspread":
                {
                    toggle("fluidspread");
                }
                case "drops":
                {
                    toggle("drops");
                }
            }
            return null;
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
        return plugin.toggles.getBoolean(toggle) ? "(enabled)" : "(disabled)";
    }

    private void toggle(String toggle)
    {
        plugin.toggles.set(toggle, !plugin.getToggles().getBoolean(toggle));
    }
}
