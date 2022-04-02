package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.cache.PlayerCache;
import dev.plex.player.PunishedPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;
import org.json.JSONTokener;
import redis.clients.jedis.Jedis;

public class PunishmentManager extends PlexBase
{
    @Getter
    private final List<IndefiniteBan> indefiniteBans = Lists.newArrayList();

    public void mergeIndefiniteBans()
    {
        this.indefiniteBans.clear();
        Plex.get().indefBans.getKeys(false).forEach(key ->
        {
            IndefiniteBan ban = new IndefiniteBan();
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

    public boolean isIndefUUIDBanned(UUID uuid)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if UUID is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().anyMatch(indefiniteBan -> indefiniteBan.getUuids().contains(uuid));
        }
        return this.indefiniteBans.stream().anyMatch(indefiniteBan -> indefiniteBan.getUuids().contains(uuid));
    }

    public boolean isIndefIPBanned(String ip)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if IP is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().anyMatch(indefiniteBan -> indefiniteBan.getIps().contains(ip));
        }
        return this.indefiniteBans.stream().anyMatch(indefiniteBan -> indefiniteBan.getIps().contains(ip));
    }

    public boolean isIndefUserBanned(String username)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            PlexLog.debug("Checking if username is banned in Redis");
            List<IndefiniteBan> bans = new Gson().fromJson(Plex.get().getRedisConnection().getJedis().get("indefbans"), new TypeToken<List<IndefiniteBan>>()
            {
            }.getType());
            return bans.stream().anyMatch(indefiniteBan -> indefiniteBan.getUsernames().contains(username));
        }
        return this.indefiniteBans.stream().anyMatch(indefiniteBan -> indefiniteBan.getUsernames().contains(username));
    }

    public void insertPunishment(PunishedPlayer player, Punishment punishment)
    {
        File file = player.getPunishmentsFile();

        try
        {
            if (isNotEmpty(file))
            {
                JSONTokener tokener = new JSONTokener(new FileInputStream(file));
                JSONObject object = new JSONObject(tokener);
                object.getJSONObject(punishment.getPunished().toString()).getJSONArray("punishments").put(punishment.toJSON());
                if (plugin.getRedisConnection().isEnabled())
                {
                    plugin.getRedisConnection().getJedis().set(player.getUuid(), object.toString());
                    PlexLog.debug("Added " + player.getUuid() + "'s punishment to the Redis database.");
                    plugin.getRedisConnection().getJedis().close();
                }

                FileWriter writer = new FileWriter(file);
                writer.append(object.toString(8));
                writer.flush();
                writer.close();
            }
            else
            {
                JSONObject object = new JSONObject();
                Map<String, List<String>> punishments = Maps.newHashMap();

                List<String> punishmentList = Lists.newArrayList();
                punishmentList.add(punishment.toJSON());

                punishments.put("punishments", punishmentList);
                object.put(punishment.getPunished().toString(), punishments);
                if (plugin.getRedisConnection().isEnabled())
                {
                    plugin.getRedisConnection().getJedis().set(player.getUuid(), object.toString());
                    PlexLog.debug("Added " + player.getUuid() + "'s punishment to the Redis database.");
                    plugin.getRedisConnection().getJedis().close();
                }

                FileWriter writer = new FileWriter(file);
                writer.append(object.toString(8));
                writer.flush();
                writer.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
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

    public boolean isBanned(UUID uuid)
    {
        return PlayerCache.getPunishedPlayer(uuid).getPunishments().stream().anyMatch(punishment -> punishment.getType() == PunishmentType.BAN && punishment.isActive());
    }

    public boolean isBanned(PunishedPlayer player)
    {
        return isBanned(UUID.fromString(player.getUuid()));
    }

    public List<Punishment> getActiveBans()
    {
        List<Punishment> punishments = Lists.newArrayList();

        if (Plex.get().getRedisConnection().isEnabled())
        {
            Jedis jedis = Plex.get().getRedisConnection().getJedis();
            jedis.keys("*").forEach(key ->
            {
                try
                {
                    UUID uuid = UUID.fromString(key);
                    String jsonPunishmentString = jedis.get(uuid.toString());
                    JSONObject object = new JSONObject(jsonPunishmentString);
                    object.getJSONObject(uuid.toString()).getJSONArray("punishments").forEach(json ->
                    {
                        Punishment punishment = Punishment.fromJson(json.toString());
                        if (punishment.isActive() && punishment.getType() == PunishmentType.BAN)
                        {
                            punishments.add(punishment);
                        }
                    });
                }
                catch (IllegalArgumentException ignored)
                {
                }
            });
        }

        return punishments;
    }

    public void unban(Punishment punishment)
    {
        this.unban(punishment.getPunished());
    }

    public void unban(UUID uuid)
    {
        if (Plex.get().getRedisConnection().isEnabled())
        {
            Jedis jedis = Plex.get().getRedisConnection().getJedis();

            String jsonPunishmentString = jedis.get(uuid.toString());
            JSONObject object = new JSONObject(jsonPunishmentString);
            List<Punishment> punishments = object.getJSONObject(uuid.toString()).getJSONArray("punishments").toList().stream().map(obj -> Punishment.fromJson(obj.toString())).collect(Collectors.toList());
            while (punishments.stream().anyMatch(punishment -> punishment.isActive() && punishment.getType() == PunishmentType.BAN))
            {
                punishments.stream().filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN).findFirst().ifPresent(punishment ->
                {
                    int index = punishments.indexOf(punishment);
                    punishment.setActive(false);
                    punishments.set(index, punishment);
                });
            }
            object.getJSONObject(uuid.toString()).getJSONArray("punishments").clear();
            object.getJSONObject(uuid.toString()).getJSONArray("punishments").putAll(punishments.stream().map(Punishment::toJSON).collect(Collectors.toList()));
            jedis.set(uuid.toString(), object.toString());
        }

        PunishedPlayer player = PlayerCache.getPunishedPlayer(uuid);

        File file = player.getPunishmentsFile();
        if (isNotEmpty(file))
        {
            try (FileInputStream fis = new FileInputStream(file))
            {
                JSONTokener tokener = new JSONTokener(fis);
                JSONObject object = new JSONObject(tokener);
                List<Punishment> punishments = object.getJSONObject(uuid.toString()).getJSONArray("punishments").toList().stream().map(obj -> Punishment.fromJson(obj.toString())).collect(Collectors.toList());
                while (punishments.stream().anyMatch(punishment -> punishment.isActive() && punishment.getType() == PunishmentType.BAN))
                {
                    punishments.stream().filter(Punishment::isActive).filter(punishment -> punishment.getType() == PunishmentType.BAN).findFirst().ifPresent(punishment ->
                    {
                        int index = punishments.indexOf(punishment);
                        punishment.setActive(false);
                        punishments.set(index, punishment);
                    });
                }
                object.getJSONObject(uuid.toString()).getJSONArray("punishments").clear();
                object.getJSONObject(uuid.toString()).getJSONArray("punishments").putAll(punishments.stream().map(Punishment::toJSON).collect(Collectors.toList()));
                FileWriter writer = new FileWriter(file);
                writer.append(object.toString());
                writer.flush();
                writer.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void issuePunishment(PunishedPlayer player, Punishment punishment)
    {
        if (punishment.getType() == PunishmentType.FREEZE)
        {
            player.setFrozen(true);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime then = punishment.getEndDate();
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
                    Bukkit.broadcast(PlexUtils.messageComponent("unfrozePlayer", "Plex", Bukkit.getOfflinePlayer(UUID.fromString(player.getUuid())).getName()));
                }
            }.runTaskLater(Plex.get(), 20 * seconds);
        }
        else if (punishment.getType() == PunishmentType.MUTE)
        {
            player.setMuted(true);
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime then = punishment.getEndDate();
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
                    Bukkit.broadcast(PlexUtils.messageComponent("unmutedPlayer", "Plex", Bukkit.getOfflinePlayer(UUID.fromString(player.getUuid())).getName()));
                }
            }.runTaskLater(Plex.get(), 20 * seconds);
        }
    }

    public void doPunishment(PunishedPlayer player, Punishment punishment)
    {
        issuePunishment(player, punishment);
        insertPunishment(player, punishment);
    }

    @Data
    public static class IndefiniteBan
    {
        private final List<String> usernames = Lists.newArrayList();
        private final List<UUID> uuids = Lists.newArrayList();
        private final List<String> ips = Lists.newArrayList();
    }
}
