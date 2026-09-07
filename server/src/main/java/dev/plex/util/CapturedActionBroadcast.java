package dev.plex.util;

import de.myzelyam.api.vanish.VanishAPI;
import dev.plex.Plex;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class CapturedActionBroadcast implements ActionBroadcast
{
    private final @Nullable Audience privateRecipients;

    private CapturedActionBroadcast(@Nullable Audience privateRecipients)
    {
        this.privateRecipients = privateRecipients;
    }

    // Capture on command entry, before work leaves the sender's region. A vanished
    // action must stay private even if its sender unvanishes or leaves before completion.
    public static ActionBroadcast capture(CommandSender sender)
    {
        if (!(sender instanceof Player actor) || !PlayerMeta.isVanished(actor))
        {
            return new CapturedActionBroadcast(null);
        }

        List<Audience> recipients = new ArrayList<>();
        recipients.add(actor);
        recipients.add(Bukkit.getConsoleSender());
        // The session snapshot avoids iterating Bukkit's live online-player view.
        for (PlexPlayer session : Plex.get().getPlayerService().cachedPlayers())
        {
            Player viewer = Bukkit.getPlayer(session.getUuid());
            if (viewer != null && !viewer.getUniqueId().equals(actor.getUniqueId()) && VanishAPI.canSee(viewer, actor))
            {
                recipients.add(viewer);
            }
        }
        return new CapturedActionBroadcast(Audience.audience(List.copyOf(recipients)));
    }

    @Override
    public void send(Component message)
    {
        if (privateRecipients == null)
        {
            Bukkit.broadcast(message);
        }
        else
        {
            privateRecipients.sendMessage(message);
        }
    }
}
