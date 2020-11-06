package me.totalfreedom.plex.punishment;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.DataUtils;
import me.totalfreedom.plex.player.PunishedPlayer;
import me.totalfreedom.plex.util.PlexLog;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class PunishmentManager
{

    public void insertPunishment(PunishedPlayer player, Punishment punishment)
    {
        File folder = new File(Plex.get().getDataFolder() + File.separator + "punishments");
        if (!folder.exists())
        {
            folder.mkdir();
        }

        File file = new File(folder, player.getUuid() + ".json");
        if (!file.exists())
        {
            try {
                file.createNewFile();
                PlexLog.log("Created new punishment file for " + player.getUuid() + " (" + DataUtils.getPlayer(punishment.getPunished()).getName() + ")");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            if (isNotEmpty(file))
            {
                JSONTokener tokener = new JSONTokener(new FileInputStream(file));
                JSONObject object = new JSONObject(tokener);
                object.getJSONObject(punishment.getPunished().toString()).getJSONArray("punishments").put(punishment.toJSON());

                FileWriter writer = new FileWriter(file);
                writer.append(object.toString(8));
                writer.flush();
                writer.close();
            } else {
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isNotEmpty(File file) {
        try {
            return !FileUtils.readFileToString(file, StandardCharsets.UTF_8).trim().isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}
