package me.totalfreedom.plex;

import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.cache.MongoPlayerData;
import me.totalfreedom.plex.cache.SQLPlayerData;
import me.totalfreedom.plex.config.Config;
import me.totalfreedom.plex.handlers.CommandHandler;
import me.totalfreedom.plex.handlers.ListenerHandler;
import me.totalfreedom.plex.message.MessageManager;
import me.totalfreedom.plex.rank.RankManager;
import me.totalfreedom.plex.storage.MongoConnection;
import me.totalfreedom.plex.storage.RedisConnection;
import me.totalfreedom.plex.storage.SQLConnection;
import me.totalfreedom.plex.storage.StorageType;
import me.totalfreedom.plex.util.PlexLog;
import me.totalfreedom.plex.util.PlexUtils;
import me.totalfreedom.plex.world.CustomWorld;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class Plex extends JavaPlugin
{
    private static Plex plugin;
    public Config config;
    private StorageType storageType = StorageType.SQLITE;

    private SQLConnection sqlConnection;
    private MongoConnection mongoConnection;
    private RedisConnection redisConnection;

    private MongoPlayerData mongoPlayerData;
    private SQLPlayerData sqlPlayerData;

    private RankManager rankManager;
    private MessageManager messageManager;

    public static Plex get()
    {
        return plugin;
    }

    @Override
    public void onLoad()
    {
        plugin = this;
        config = new Config(this, "config.yml");
        saveResource("database.db", false);

        sqlConnection = new SQLConnection();
        mongoConnection = new MongoConnection();
        redisConnection = new RedisConnection();
        /*try {
            redisConnection.openPool();
            PlexLog.log("Successfully opened redis pool. Closing.");
        } catch (Exception e) {
            e.printStackTrace();
        }
        redisConnection.getJedis().close();*/
    }

    @Override
    public void onEnable()
    {
        config.load();

        try
        {
            PlexUtils.testConnections();
            PlexLog.log("Connected to " + storageType.name().toUpperCase());
        }
        catch (Exception e)
        {
            PlexLog.error("Failed to connect to " + storageType.name().toUpperCase());
            e.printStackTrace();
        }

        if (storageType == StorageType.MONGO)
        {
            mongoPlayerData = new MongoPlayerData();
        }
        else
        {
            sqlPlayerData = new SQLPlayerData();
        }

        new ListenerHandler(); // this doesn't need a variable.
        new CommandHandler();

        rankManager = new RankManager();
        rankManager.generateDefaultRanks();
        rankManager.importDefaultRanks();
        PlexLog.log("Rank Manager initialized");

        messageManager = new MessageManager();
        messageManager.generateMessages();
        PlexLog.log("Message Manager initialized");

        generateWorlds();
    }

    @Override
    public void onDisable()
    {
        /*if (redisConnection.getJedis().isConnected())
        {
            PlexLog.log("Disabling Redis/Jedis. No memory leaks in this Anarchy server !");
            redisConnection.getJedis().close();
        }*/
    }

    private void generateWorlds()
    {
        PlexLog.log("Generating any worlds if needed...");
        for (String key : config.getConfigurationSection("worlds").getKeys(false))
            CustomWorld.generateConfigFlatWorld(key);
        PlexLog.log("Finished with world generation!");
    }
}