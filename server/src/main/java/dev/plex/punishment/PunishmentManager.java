package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class PunishmentManager
{
    private final Plex plugin;

    public PunishmentManager(Plex plugin)
    {
        this.plugin = plugin;
    }
    @Getter
    private final List<IndefiniteBan> indefiniteBans = Lists.newArrayList();

    public void mergeIndefiniteBans()
    {
        this.indefiniteBans.clear();
        plugin.indefBans.getKeys(false).forEach(key ->
        {
            IndefiniteBan ban = new IndefiniteBan(plugin.getIndefBans().getString("reason", ""));
            ban.ips.addAll(plugin.getIndefBans().getStringList(key + ".ips"));
            ban.usernames.addAll(plugin.getIndefBans().getStringList(key + ".users"));
            ban.uuids.addAll(plugin.getIndefBans().getStringList(key + ".uuids").stream().map(UUID::fromString).toList());
            this.indefiniteBans.add(ban);
        });

        PlexLog.log("Loaded {0} UUID(s), {1} IP(s), and {2} username(s) as indefinitely banned", this.indefiniteBans.stream().map(IndefiniteBan::getUuids).mapToLong(Collection::size).sum(), this.indefiniteBans.stream().map(IndefiniteBan::getIps).mapToLong(Collection::size).sum(), this.indefiniteBans.stream().map(IndefiniteBan::getUsernames).mapToLong(Collection::size).sum());

        if (plugin.getRedisConnection().isEnabled())
        {
            PlexLog.log("Asynchronously uploading all indefinite bans to Redis");
            plugin.getRedisConnection().runAsync(jedis ->
            {
                jedis.set("indefbans", new Gson().toJson(indefiniteBans));
            });
        }
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUUID(UUID uuid)
    {
        if (plugin.getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if UUID is banned in Redis");
            List<IndefiniteBan> bans = redisIndefiniteBans();
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getUuids().contains(uuid)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getUuids().contains(uuid)).findFirst().orElse(null);
    }

    public IndefiniteBan getIndefiniteBanByIP(String ip)
    {
        if (plugin.getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if IP is banned in Redis");
            List<IndefiniteBan> bans = redisIndefiniteBans();
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getIps().contains(ip)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getIps().contains(ip)).findFirst().orElse(null);
    }

    public IndefiniteBan getIndefiniteBanByUsername(String username)
    {
        if (plugin.getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if username is banned in Redis");
            List<IndefiniteBan> bans = redisIndefiniteBans();
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getUsernames().contains(username)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getUsernames().contains(username)).findFirst().orElse(null);
    }

    public void issuePunishment(PlexPlayer plexPlayer, Punishment punishment)
    {
        plexPlayer.getPunishments().add(punishment);
        plugin.getPunishmentRepository().insertPunishment(punishment);
    }

    private List<IndefiniteBan> redisIndefiniteBans()
    {
        String json = plugin.getRedisConnection().query(jedis -> jedis.get("indefbans"));
        List<IndefiniteBan> bans = new Gson().fromJson(json, new TypeToken<List<IndefiniteBan>>()
        {
        }.getType());
        return bans == null ? Lists.newArrayList() : bans;
    }

    private boolean isNotEmpty(File file)
    {
        try
        {
            return !FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim().isEmpty();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    public CompletableFuture<Boolean> isAsyncBanned(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            if (!plugin.getPlayerService().hasPlayedBefore(uuid))
            {
                return false;
            }

            return plugin.getPunishmentRepository().getPunishments(uuid).stream().anyMatch(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive());
        }, plugin.getApi().scheduler().asyncExecutor());
    }

    public boolean isBanned(UUID uuid)
    {
        if (!plugin.getPlayerService().hasPlayedBefore(uuid))
        {
            return false;
        }
        return plugin.getPlayerService().getPlayer(uuid).getPunishments().stream().anyMatch(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive());
    }

    public Punishment getBanByIP(String ip)
    {
        return plugin.getPunishmentRepository().getPunishments(ip).stream().filter(punishment -> punishment.getType() == PunishmentType.TEMPBAN || punishment.getType() == PunishmentType.BAN).filter(Punishment::isActive).filter(punishment -> punishment.getIp().equals(ip)).findFirst().orElse(null);
    }

    public boolean isBanned(PlexPlayer player)
    {
        return isBanned(player.getUuid());
    }

    public CompletableFuture<List<Punishment>> getActiveBans()
    {
        //PlexLog.debug("Checking active bans mysql");
        CompletableFuture<List<Punishment>> future = new CompletableFuture<>();
        plugin.getPunishmentRepository().getPunishments().whenComplete((punishments, throwable) ->
        {
            //PlexLog.debug("Received Punishments");
            List<Punishment> punishmentList = punishments.stream().filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN).toList();
            //PlexLog.debug("Completing with {0} punishments", punishmentList.size());
            future.complete(punishmentList);
        });
        return future;
    }

    public void unban(Punishment punishment)
    {
        this.unban(punishment.getPunished());
    }

    public CompletableFuture<Void> unban(UUID uuid)
    {
        return plugin.getPunishmentRepository().removeBan(uuid);
    }

    public void updateOutdatedPunishments(PlexPlayer player)
    {

    }

    private void doPunishment(PlexPlayer player, Punishment punishment)
    {
        if (punishment.getType() == PunishmentType.FREEZE)
        {
            player.setFrozen(true);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            ZonedDateTime then = punishment.getEndDate();
            long seconds = ChronoUnit.SECONDS.between(now, then);
            plugin.getApi().scheduler().runGlobalLater(scheduledTask ->
            {
                PlexPlayer afterPlayer = plugin.getPlayerService().getPlayer(player.getUuid());
                if (!afterPlayer.isFrozen())
                {
                    return;
                }
                afterPlayer.setFrozen(false);
                punishment.setActive(false);
                plugin.getPunishmentRepository().updatePunishment(punishment.getType(), false, punishment.getPunished());

                plugin.getPlayerService().update(afterPlayer);
                Bukkit.broadcast(PlexUtils.messageComponent("unfrozePlayer", "Plex", Bukkit.getOfflinePlayer(afterPlayer.getUuid()).getName()));
            }, Math.max(1L, 20L * seconds));
        }
        else if (punishment.getType() == PunishmentType.MUTE)
        {
            player.setMuted(true);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            ZonedDateTime then = punishment.getEndDate();
            long seconds = ChronoUnit.SECONDS.between(now, then);
            plugin.getApi().scheduler().runGlobalLater(scheduledTask ->
            {
                PlexPlayer afterPlayer = plugin.getPlayerService().getPlayer(player.getUuid());
                if (!afterPlayer.isMuted())
                {
                    return;
                }
                afterPlayer.setMuted(false);
                punishment.setActive(false);
                plugin.getPunishmentRepository().updatePunishment(punishment.getType(), false, punishment.getPunished());

                Bukkit.broadcast(PlexUtils.messageComponent("unmutedPlayer", "Plex", Bukkit.getOfflinePlayer(afterPlayer.getUuid()).getName()));
            }, Math.max(1L, 20L * seconds));
        }
    }

    public void punish(PlexPlayer player, Punishment punishment)
    {
        issuePunishment(player, punishment);
        doPunishment(player, punishment);
    }

    @Data
    public static class IndefiniteBan
    {
        private final List<String> usernames = Lists.newArrayList();
        private final List<UUID> uuids = Lists.newArrayList();
        private final List<String> ips = Lists.newArrayList();
        private final String reason;
    }
}
