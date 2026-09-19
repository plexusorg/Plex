package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentManager;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.util.PlexLog;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import org.bukkit.entity.Player;

public class BanListener extends ServerListenerBase
{
    private final ConcurrentHashMap<AsyncPlayerPreLoginEvent, Long> admissionTokens = new ConcurrentHashMap<>();

    public BanListener(Plex plugin) { super(plugin); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        PunishmentManager.IndefiniteBan indefinite = plugin.getPunishmentManager().getIndefiniteBanByUUID(event.getUniqueId());
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "UUID");
            return;
        }

        final String ip = event.getAddress().getHostAddress();
        indefinite = plugin.getPunishmentManager().getIndefiniteBanByIP(ip);
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "IP");
            return;
        }
        indefinite = plugin.getPunishmentManager().getIndefiniteBanByUsername(event.getName());
        if (indefinite != null)
        {
            disallowIndefinite(event, indefinite, "username");
            return;
        }

        Punishment punishment;
        BanDecisionService.Revision decisionRevision;
        try
        {
            long token;
            do
            {
                decisionRevision = plugin.getPunishmentManager().banDecisionRevision(event.getUniqueId(), ip);
                punishment = plugin.getPunishmentManager().decideAdmission(event.getUniqueId(), ip).join().orElse(null);
                token = plugin.getPunishmentManager().prepareFiniteBanAdmission(event.getUniqueId(), ip, punishment,
                        decisionRevision);
            }
            while (token == -2L);
            if (token < 0L)
            {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                        Component.text("Someone is already logging in with your account."));
                return;
            }
            admissionTokens.put(event, token);
        }
        catch (CompletionException failure)
        {
            PlexLog.error("Unable to evaluate admission for {0}: {1}", event.getUniqueId(), failure.getCause() == null ? failure.getMessage() : failure.getCause().getMessage());
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Unable to verify your ban status. Please try again shortly."));
            return;
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPreLoginComplete(AsyncPlayerPreLoginEvent event)
    {
        Long token = admissionTokens.remove(event);
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED)
        {
            if (token != null) plugin.getPunishmentManager().cancelPendingAdmission(event.getUniqueId(), token);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onServerFullCheck(PlayerServerFullCheckEvent event)
    {
        plugin.getPunishmentManager().checkAdmissionCapacity(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event)
    {
        plugin.getPunishmentManager().completeJoin(event.getPlayer());
        if (plugin.getPunishmentManager().isFiniteBanRestricted(event.getPlayer().getUniqueId()))
        {
            event.joinMessage(null);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event)
    {
        if (plugin.getPunishmentManager().isFiniteBanRestricted(event.getPlayer().getUniqueId()))
        {
            event.quitMessage(null);
        }
        plugin.getPunishmentManager().completeQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onConnectionClosed(PlayerConnectionCloseEvent event)
    {
        plugin.getPunishmentManager().closePendingAdmission(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        blockInteraction(event.getPlayer().getUniqueId(), event.getPlayer(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        UUID uuid = event.getPlayer().getUniqueId();
        boolean restricted = blockInteraction(uuid, event.getPlayer(), event);
        PlexLog.debug("Command ban check for {0}: restricted={1}, cancelled={2}",
                uuid, restricted, event.isCancelled());
    }

    private boolean blockInteraction(UUID uuid, Player player, Cancellable event)
    {
        if (!plugin.getPunishmentManager().isFiniteBanRestricted(uuid)) return false;
        event.setCancelled(true);
        player.sendMessage(plugin.getPunishmentManager().finiteBanMessage(uuid));
        return true;
    }

    private void disallowIndefinite(AsyncPlayerPreLoginEvent event, PunishmentManager.IndefiniteBan ban, String type)
    {
        String reason = ban.getReason();
        Component message = reason.isEmpty()
                ? Punishment.generateIndefBanMessage(type, plugin.config.getString("banning.ban_url"))
                : Punishment.generateIndefBanMessageWithReason(type, plugin.config.getString("banning.ban_url"), reason);
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, message);
    }
}
