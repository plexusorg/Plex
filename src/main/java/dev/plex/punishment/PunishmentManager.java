package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.plex.Plex;
import dev.plex.banning.Ban;
import dev.plex.player.PunishedPlayer;
import dev.plex.util.PlexUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;
import org.json.JSONTokener;

public class PunishmentManager
{

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

    private void issuePunishment(PunishedPlayer player, Punishment punishment)
    {
        if (punishment.getType() == PunishmentType.BAN)
        {
            Ban ban = new Ban(punishment.getPunished(), (punishment.getPunisher() == null ? null : punishment.getPunisher()), "", punishment.getReason(), punishment.getEndDate());
            Plex.get().getBanManager().executeBan(ban);
        }
        else if (punishment.getType() == PunishmentType.FREEZE)
        {
            player.setFrozen(true);
            Date now = new Date();
            Date then = punishment.getEndDate();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(then.getTime() - now.getTime());
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
                    Bukkit.broadcastMessage(PlexUtils.tl("unfrozePlayer", "Plex", Bukkit.getOfflinePlayer(UUID.fromString(player.getUuid())).getName()));
                    Bukkit.getLogger().info("Unfroze");
                }
            }.runTaskLater(Plex.get(), 20 * seconds);


        }
        else if (punishment.getType() == PunishmentType.MUTE)
        {
            player.setMuted(true);
            Date now = new Date();
            Date then = punishment.getEndDate();
            long seconds = TimeUnit.MILLISECONDS.toSeconds(then.getTime() - now.getTime());
            new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    player.setMuted(false);
                }
            }.runTaskLater(Plex.get(), 20 * seconds);
        }
    }

    public void doPunishment(PunishedPlayer player, Punishment punishment)
    {
        issuePunishment(player, punishment);
        insertPunishment(player, punishment);
    }

}
