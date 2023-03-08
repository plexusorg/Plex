package dev.plex.command.impl;

import com.google.common.base.Splitter;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@CommandParameters(name = "totalfreedommod", description = "You can't simpy do that.", aliases = "tfm")
@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)

public class TFMCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (playerSender != null)
        {
            String simpy = "It's not about credit or discredit, it's about copyright of TFM. Looking at Plex's source code, it looks like a bunch of copy-pasted source code from TFM and Aero (utility plugin). You just put it under a different license and pretended it's yours. You can't simpy do that. You have to remove all parts which infringe on the TFM license (as per the TFM repository) and the Aero License (as per the Aero repository) or otherwise comply to the license requirements.";
            List<String> pages = Splitter.fixedLength(256).splitToList(simpy);
            List<Component> pageComponents = new ArrayList<>();

            for (String page : pages)
            {
                pageComponents.add(PlexUtils.mmDeserialize("<rainbow>" + page + "</rainbow>"));
            }

            playerSender.openBook(Book.builder()
                    .title(Component.text("TFM License"))
                    .author(Component.text("Prozza"))
                    .pages(pageComponents));
        }

        return PlexUtils.mmDeserialize("<rainbow>It's not about credit or discredit, it's about copyright of TFM.<br>Looking at Plex's source code, it looks like a bunch of copy-pasted source code from TFM and Aero (utility plugin). You just put it under a different license and pretended it's yours.<br>You can't simpy do that. You have to remove all parts which infringe on the TFM license (as per the TFM repository) and the Aero License (as per the Aero repository) or otherwise comply to the license requirements.</rainbow>");
    }
}
