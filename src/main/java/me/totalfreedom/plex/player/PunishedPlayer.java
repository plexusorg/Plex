package me.totalfreedom.plex.player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.DataUtils;
import me.totalfreedom.plex.event.PunishedPlayerFreezeEvent;
import me.totalfreedom.plex.event.PunishedPlayerMuteEvent;
import me.totalfreedom.plex.punishment.Punishment;
import me.totalfreedom.plex.util.PlexLog;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.json.JSONObject;
import org.json.JSONTokener;

@Getter
public class PunishedPlayer
{
    //everything in here will be stored in redis
    @Setter(AccessLevel.NONE)
    private String uuid;

    private boolean muted;
    private boolean frozen;

    public PunishedPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();
        this.muted = false;
        this.frozen = false;
    }

    public void setFrozen(boolean frozen)
    {
        PunishedPlayerFreezeEvent e = new PunishedPlayerFreezeEvent(this, this.frozen);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
        {
            this.frozen = frozen;
        }
    }

    public void setMuted(boolean muted)
    {
        PunishedPlayerMuteEvent e = new PunishedPlayerMuteEvent(this, this.muted);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
        {
            this.muted = muted;
        }
    }

    public File getPunishmentsFile()
    {
        File folder = new File(Plex.get().getDataFolder() + File.separator + "punishments");
        if (!folder.exists())
        {
            folder.mkdir();
        }

        File file = new File(folder, getUuid() + ".json");
        if (!file.exists())
        {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return file;

    }

    public List<Punishment> getPunishments()
    {
        List<Punishment> punishments = Lists.newArrayList();

        File file = getPunishmentsFile();

        if (isNotEmpty(file))
        {
            try {
                JSONTokener tokener = new JSONTokener(new FileInputStream(file));
                JSONObject object = new JSONObject(tokener);
                object.getJSONObject(getUuid()).getJSONArray("punishments").forEach(obj -> {
                    Punishment punishment = Punishment.fromJson(obj.toString());
                    punishments.add(punishment);
                });
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return punishments;
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
