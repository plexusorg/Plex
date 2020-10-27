package me.totalfreedom.plex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.config.Config;
import me.totalfreedom.plex.config.YamlConfig;
import me.totalfreedom.plex.storage.MongoConnection;
import me.totalfreedom.plex.storage.SQLConnection;
import me.totalfreedom.plex.storage.StorageType;
import me.totalfreedom.plex.util.PlexLog;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class Plex extends JavaPlugin
{
    @Setter(AccessLevel.NONE)
    private static Plex plugin;

    private StorageType storageType;

    private SQLConnection sqlConnection;
    private MongoConnection mongoConnection;

    @Override
    public void onLoad()
    {
        plugin = this;

        getConfig().options().copyDefaults(true);
        saveConfig();

        saveResource("database.db", false);

        sqlConnection = new SQLConnection();
        mongoConnection = new MongoConnection();
    }

    @Override
    public void onEnable()
    {
        PlexUtils.testConnections();
    }

    @Override
    public void onDisable()
    {
    }

    public static Plex get() {
        return plugin;
    }
}