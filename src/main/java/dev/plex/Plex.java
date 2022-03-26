package dev.plex;

import dev.plex.admin.Admin;
import dev.plex.admin.AdminList;
import dev.plex.cache.DataUtils;
import dev.plex.cache.MongoPlayerData;
import dev.plex.cache.PlayerCache;
import dev.plex.cache.SQLPlayerData;
import dev.plex.config.Config;
import dev.plex.handlers.CommandHandler;
import dev.plex.handlers.ListenerHandler;
import dev.plex.module.ModuleManager;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.PunishmentManager;
import dev.plex.rank.RankManager;
import dev.plex.services.ServiceManager;
import dev.plex.storage.MongoConnection;
import dev.plex.storage.RedisConnection;
import dev.plex.storage.SQLConnection;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.UpdateChecker;
import dev.plex.world.CustomWorld;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class Plex extends JavaPlugin
{
    private static Plex plugin;
    public Config config;
    public Config messages;
    public Config indefBans;

    public File modulesFolder;

    private StorageType storageType = StorageType.SQLITE;

    private SQLConnection sqlConnection;
    private MongoConnection mongoConnection;
    private RedisConnection redisConnection;

    private MongoPlayerData mongoPlayerData;
    private SQLPlayerData sqlPlayerData;

    private ModuleManager moduleManager;
    private RankManager rankManager;
    private ServiceManager serviceManager;

    private PunishmentManager punishmentManager;

    private AdminList adminList;

    private UpdateChecker updateChecker;

    private String system;

    public static final BuildProperties build = new BuildProperties();

    public static Plex get()
    {
        return plugin;
    }

    @Override
    public void onLoad()
    {
        plugin = this;
        config = new Config(this, "config.yml");
        messages = new Config(this, "messages.yml");
        indefBans = new Config(this, "indefbans.yml");
        build.load(this);

        modulesFolder = new File(this.getDataFolder() + File.separator + "modules");
        if (!modulesFolder.exists())
        {
            modulesFolder.mkdir();
        }

        sqlConnection = new SQLConnection();
        mongoConnection = new MongoConnection();
        redisConnection = new RedisConnection();

        moduleManager = new ModuleManager();
        moduleManager.loadAllModules();
        moduleManager.loadModules();
    }

    @Override
    public void onEnable()
    {
        config.load();
        messages.load();
        // Don't add default entries to indefinite ban file
        indefBans.load(false);

        moduleManager.enableModules();

        system = config.getString("commands.permissions");

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

        updateChecker = new UpdateChecker();
        PlexLog.log("Update checking enabled");

        // https://bstats.org/plugin/bukkit/Plex/14143
        Metrics metrics = new Metrics(this, 14143);
        PlexLog.log("Enabled Metrics");

        if (redisConnection.isEnabled())
        {
            redisConnection.getJedis();
            PlexLog.log("Connected to Redis!");
        }
        else
        {
            PlexLog.log("Redis is disabled in the configuration file, not connecting.");
        }

        if (storageType == StorageType.MONGODB)
        {
            mongoPlayerData = new MongoPlayerData();
        }
        else
        {
            sqlPlayerData = new SQLPlayerData();
        }

        new ListenerHandler();
        new CommandHandler();

        rankManager = new RankManager();
        rankManager.generateDefaultRanks();
        rankManager.importDefaultRanks();
        adminList = new AdminList();
        PlexLog.log("Rank Manager initialized");

        punishmentManager = new PunishmentManager();
        punishmentManager.mergeIndefiniteBans();
        PlexLog.log("Punishment System initialized");

        generateWorlds();

        serviceManager = new ServiceManager();
        PlexLog.log("Service Manager initialized");
        serviceManager.startServices();
        PlexLog.log("Started " + serviceManager.serviceCount() + " services.");

        reloadPlayers();
    }

    @Override
    public void onDisable()
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId()); //get the player because it's literally impossible for them to not have an object

            if (plugin.getRankManager().isAdmin(plexPlayer))
            {
                plugin.getAdminList().removeFromCache(UUID.fromString(plexPlayer.getUuid()));
            }

            if (mongoPlayerData != null) //back to mongo checking
            {
                mongoPlayerData.update(plexPlayer); //update the player's document
            }
            else if (sqlPlayerData != null) //sql checking
            {
                sqlPlayerData.update(plexPlayer);
            }
        });
        if (redisConnection.isEnabled() && redisConnection.getJedis().isConnected())
        {
            PlexLog.log("Disabling Redis/Jedis. No memory leaks in this Anarchy server!");
            redisConnection.getJedis().close();
        }

        moduleManager.disableModules();
    }

    private void generateWorlds()
    {
        PlexLog.log("Generating any worlds if needed...");
        for (String key : config.getConfigurationSection("worlds").getKeys(false))
        {
            CustomWorld.generateConfigFlatWorld(key);
        }
        PlexLog.log("Finished with world generation!");
    }

    private void reloadPlayers()
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
            PlayerCache.getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
            PlayerCache.getPunishedPlayerMap().put(player.getUniqueId(), new PunishedPlayer(player.getUniqueId()));
            if (plugin.getRankManager().isAdmin(plexPlayer))
            {
                Admin admin = new Admin(UUID.fromString(plexPlayer.getUuid()));
                admin.setRank(plexPlayer.getRankFromString());

                plugin.getAdminList().addToCache(admin);
            }
        });
    }

    public static class BuildProperties
    {
        public String number;
        public String author;
        public String date;
        public String head;

        public void load(Plex plugin)
        {
            try
            {
                final Properties props;

                try (InputStream in = plugin.getResource("build.properties"))
                {
                    props = new Properties();
                    props.load(in);
                }

                number = props.getProperty("buildNumber", "unknown");
                author = props.getProperty("buildAuthor", "unknown");
                date = props.getProperty("buildDate", "unknown");
                head = props.getProperty("buildHead", "unknown");
            }
            catch (Exception ex)
            {
                PlexLog.error("Could not load build properties! Did you compile with NetBeans/Maven?");
            }
        }
    }
}