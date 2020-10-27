package me.totalfreedom.plex;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.cache.MongoPlayerData;
import me.totalfreedom.plex.cache.SQLPlayerData;
import me.totalfreedom.plex.config.Config;
import me.totalfreedom.plex.config.YamlConfig;
import me.totalfreedom.plex.listeners.PlayerListener;
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

    private StorageType storageType = StorageType.SQLITE;

    private SQLConnection sqlConnection;
    private MongoConnection mongoConnection;

    private MongoPlayerData mongoPlayerData;
    private SQLPlayerData sqlPlayerData;

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

        if (storageType == StorageType.MONGO)
        {
            mongoPlayerData = new MongoPlayerData();
        } else {
            sqlPlayerData = new SQLPlayerData();
        }

        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        PlexLog.log(storageType.name());
    }

    @Override
    public void onDisable()
    {
    }

    public static Plex get() {
        return plugin;
    }
}