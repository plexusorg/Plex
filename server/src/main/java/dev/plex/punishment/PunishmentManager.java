package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.cache.DataUtils;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

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
import java.util.stream.Collectors;

public class PunishmentManager implements PlexBase
{
    @Getter
    private final List<IndefiniteBan> indefiniteBans = Lists.newArrayList();

    public void mergeIndefiniteBans()
    {
        this.indefiniteBans.clear();
        Plex.get().indefBans.getKeys(false).forEach(key ->
        {
            IndefiniteBan ban = new IndefiniteBan(Plex.get().getIndefBans().getString("reason", ""));
            ban.ips.addAll(Plex.get().getIndefBans().getStringList(key + ".ips"));
            ban.usernames.addAll(Plex.get().getIndefBans().getStringList(key + ".users"));
            ban.uuids.addAll(Plex.get().getIndefBans().getStringList(key + ".uuids").stream().map(UUID::fromString).toList());
            this.indefiniteBans.add(ban);
        });

        PlexLog.log("Loaded {0} UUID(s), {1} IP(s), and {2} username(s) as indefinitely banned", this.indefiniteBans.stream().map(IndefiniteBan::getUuids).mapToLong(Collection::size).sum(), this.indefiniteBans.stream().map(IndefiniteBan::getIps).mapToLong(Collection::size).sum(), this.indefiniteBans.stream().map(IndefiniteBan::getUsernames).mapToLong(Collection::size).sum());

        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.log("Asynchronously uploading all indefinite bans to Redis");
            Plex.get().getRedisConnection().runAsync(jedis ->
            {
                jedis.set("indefbans", new Gson().toJson(indefiniteBans));
            });
        }
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUUID(UUID uuid)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if UUID is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getUuids().contains(uuid)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getUuids().contains(uuid)).findFirst().orElse(null);
    }

    public IndefiniteBan getIndefiniteBanByIP(String ip)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if IP is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getIps().contains(ip)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getIps().contains(ip)).findFirst().orElse(null);
    }

    public IndefiniteBan getIndefiniteBanByUsername(String username)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if username is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().filter(indefiniteBan -> indefiniteBan.getUsernames().contains(username)).findFirst().orElse(null);
        }
        return this.indefiniteBans.stream().filter(indefiniteBan -> indefiniteBan.getUsernames().contains(username)).findFirst().orElse(null);
    }

    public void issuePunishment(PlexPlayer plexPlayer, Punishment punishment)
    {
        plexPlayer.getPunishments().add(punishment);
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            CompletableFuture.runAsync(() ->
            {
                DataUtils.update(plexPlayer);
            });
        }
        else
        {
            Plex.get().getSqlPunishment().insertPunishment(punishment);
        }
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
            if (!DataUtils.hasPlayedBefore(uuid))
            {
                return false;
            }

            PlexPlayer player = DataUtils.getPlayer(uuid);
            player.loadPunishments();
            return player.getPunishments().stream().anyMatch(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive());
        });
    }

    public boolean isBanned(UUID uuid)
    {
        // TODO: If a person is using MongoDB, this will error out because it is checking for bans on a player that doesn't exist yet
        if (!DataUtils.hasPlayedBefore(uuid))
        {
            return false;
        }
        return DataUtils.getPlayer(uuid).getPunishments().stream().anyMatch(punishment -> (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) && punishment.isActive());
    }

    public boolean isBanned(PlexPlayer player)
    {
        return isBanned(player.getUuid());
    }

    public CompletableFuture<List<Punishment>> getActiveBans()
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return CompletableFuture.supplyAsync(() ->
            {
                List<PlexPlayer> players = Plex.get().getMongoPlayerData().getPlayers();
                return players.stream().map(PlexPlayer::getPunishments).flatMap(Collection::stream).filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN).toList();
            });
        }
        else
        {
            //PlexLog.debug("Checking active bans mysql");
            CompletableFuture<List<Punishment>> future = new CompletableFuture<>();
            Plex.get().getSqlPunishment().getPunishments().whenComplete((punishments, throwable) ->
            {
                //PlexLog.debug("Received Punishments");
                List<Punishment> punishmentList = punishments.stream().filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN).toList();
                //PlexLog.debug("Completing with {0} punishments", punishmentList.size());
                future.complete(punishmentList);
            });
            return future;
        }
    }

    public void unban(Punishment punishment)
    {
        this.unban(punishment.getPunished());
    }

    public CompletableFuture<Void> unban(UUID uuid)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return CompletableFuture.runAsync(() ->
            {
                PlexPlayer plexPlayer = DataUtils.getPlayer(uuid);
                plexPlayer.setPunishments(plexPlayer.getPunishments().stream().filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN)
                        .peek(punishment -> punishment.setActive(false)).collect(Collectors.toList()));
                DataUtils.update(plexPlayer);
            });
        }
        else
        {
            return Plex.get().getSqlPunishment().removeBan(uuid);
        }
    }

    private void doPunishment(PlexPlayer player, Punishment punishment)
    {
        if (punishment.getType() == PunishmentType.FREEZE)
        {
            player.setFrozen(true);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            ZonedDateTime then = punishment.getEndDate();
            long seconds = ChronoUnit.SECONDS.between(now, then);
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    if (!player.isFrozen())
                    {
                        this.cancel();
                        return;
                    }
                    player.setFrozen(false);
                    Bukkit.broadcast(PlexUtils.messageComponent("unfrozePlayer", "Plex", Bukkit.getOfflinePlayer(player.getUuid()).getName()));
                }
            }.runTaskLater(Plex.get(), 20 * seconds);
        }
        else if (punishment.getType() == PunishmentType.MUTE)
        {
            player.setMuted(true);
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            ZonedDateTime then = punishment.getEndDate();
            long seconds = ChronoUnit.SECONDS.between(now, then);
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    if (!player.isMuted())
                    {
                        this.cancel();
                        return;
                    }
                    player.setMuted(false);
                    Bukkit.broadcast(PlexUtils.messageComponent("unmutedPlayer", "Plex", Bukkit.getOfflinePlayer(player.getUuid()).getName()));
                }
            }.runTaskLater(Plex.get(), 20 * seconds);
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
