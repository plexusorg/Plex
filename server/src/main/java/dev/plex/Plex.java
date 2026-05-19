package dev.plex;

import dev.plex.api.PlexApi;
import dev.plex.api.impl.DefaultPlexApi;
import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.command.ServerCommand;
import dev.plex.config.Config;
import dev.plex.config.ModuleConfig;
import dev.plex.handlers.CommandHandler;
import dev.plex.handlers.ListenerHandler;
import dev.plex.hook.CoreProtectHook;
import dev.plex.hook.PrismHook;
import dev.plex.module.ModuleManager;
import dev.plex.player.PlayerService;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.PunishmentManager;
import dev.plex.services.ServiceManager;
import dev.plex.storage.RedisConnection;
import dev.plex.storage.SQLConnection;
import dev.plex.storage.StorageExecutor;
import dev.plex.storage.StorageType;
import dev.plex.storage.player.SQLPlayerData;
import dev.plex.storage.punishment.SQLNotes;
import dev.plex.storage.punishment.SQLPunishment;
import dev.plex.storage.repository.NoteRepository;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.util.BuildInfo;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.UpdateChecker;
import dev.plex.util.redis.MessageUtil;
import dev.plex.world.CustomWorld;

import java.io.File;

import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
@Setter
public class Plex extends JavaPlugin
{
    public static final BuildInfo build = new BuildInfo();
    public static final int MODULE_API_COMPATIBILITY_VERSION = 1;
    private static Plex plugin;
    public Config config;
    public Config messages;
    public Config indefBans;
    public Config toggles;
    public File modulesFolder;
    private StorageType storageType = StorageType.SQLITE;
    private SQLConnection sqlConnection;
    private RedisConnection redisConnection;

    private PlayerCache playerCache;
    private PlayerRepository playerRepository;
    private PlayerService playerService;

    private PunishmentRepository punishmentRepository;
    private NoteRepository noteRepository;

    private ModuleManager moduleManager;
    private ServiceManager serviceManager;
    private PunishmentManager punishmentManager;
    private UpdateChecker updateChecker;
    private PlexApi api;

    private Permission permissions;
    private Chat chat;

    private CoreProtectHook coreProtectHook;
    private PrismHook prismHook;

    public static Plex get()
    {
        return plugin;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        plugin = this;
        config = new Config(this, "config.yml");
        messages = new Config(this, "messages.yml");
        indefBans = new Config(this, "indefbans.yml");
        toggles = new Config(this, "toggles.yml");
        build.load(this);
        api = new DefaultPlexApi(this, MODULE_API_COMPATIBILITY_VERSION);
        installModuleApiRuntimes();

        modulesFolder = new File(this.getDataFolder() + File.separator + "modules");
        if (!modulesFolder.exists())
        {
            modulesFolder.mkdir();
        }

        moduleManager = new ModuleManager(this);
        moduleManager.loadAllModules();
        moduleManager.loadModules();

        //this.setChatHandler(new ChatListener.ChatHandlerImpl());
    }

    private void installModuleApiRuntimes()
    {
        ServerCommand.setRuntime(new ServerCommand.Runtime()
        {
            @Override
            public Plex plugin()
            {
                return Plex.this;
            }

            @Override
            public void register(org.bukkit.command.Command command)
            {
                api.commands().register(command);
            }
        });
        PlexCommand.setRuntime(new PlexCommand.Runtime()
        {
            @Override
            public void register(org.bukkit.command.Command command)
            {
                api.commands().register(command);
            }

            @Override
            public net.kyori.adventure.text.Component messageComponent(String entry, Object... objects)
            {
                return api.messages().messageComponent(entry, objects);
            }

            @Override
            public net.kyori.adventure.text.Component messageComponent(String entry, net.kyori.adventure.text.Component... objects)
            {
                return api.messages().messageComponent(entry, objects);
            }

            @Override
            public String messageString(String entry, Object... objects)
            {
                return api.messages().messageString(entry, objects);
            }

            @Override
            public net.kyori.adventure.text.Component miniMessage(String input)
            {
                return api.messages().miniMessage(input);
            }
        });
        ModuleConfig.setFactory((module, from, to) -> api.moduleConfigs().create(module, from, to));
    }

    @Override
    public void onEnable()
    {
        config.load();
        PlexLog.setDebugEnabled(config.getBoolean("debug"));
        messages.load();
        PlexUtils.configure(config, messages);
        toggles.load();

        // Don't add default entries to these files
        indefBans.load(false);

        sqlConnection = new SQLConnection(this);
        redisConnection = new RedisConnection(this);

        playerCache = new PlayerCache();

        PlexLog.log("Attempting to connect to DB: {0}", plugin.config.getString("data.db.name"));
        try
        {
            PlexUtils.testConnections(this);
            PlexLog.log("Connected to " + storageType.name().toUpperCase());
        }
        catch (Exception e)
        {
            PlexLog.error("Failed to connect to " + storageType.name().toUpperCase());
            e.printStackTrace();
        }

        if (!getServer().getPluginManager().isPluginEnabled("Vault"))
        {
            throw new RuntimeException("Vault is required to run on the server alongside a permissions plugin, we recommend LuckPerms!");
        }

        permissions = setupPermissions();
        chat = setupChat();

        if (plugin.getServer().getPluginManager().isPluginEnabled("CoreProtect"))
        {
            PlexLog.log("Hooked into CoreProtect!");
            coreProtectHook = new CoreProtectHook(this);
        }
        else
        {
            PlexLog.debug("Not hooking into CoreProtect");
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("Prism"))
        {
            PlexLog.log("Hooked into Prism!");
            prismHook = new PrismHook(this);
        }
        else
        {
            PlexLog.debug("Not hooking into Prism");
        }

        if (PlexUtils.hasVanishPlugin())
        {
            PlexLog.log("Hooked into SuperVanish / PremiumVanish!");
        }
        else
        {
            PlexLog.debug("Not hooking into SuperVanish / PremiumVanish");
        }

        updateChecker = new UpdateChecker(this);
        PlexLog.log("Update checking enabled");

        // https://bstats.org/plugin/bukkit/Plex/14143
        Metrics metrics = new Metrics(this, 14143);
        PlexLog.log("Enabled Metrics");

        if (redisConnection != null && redisConnection.isEnabled())
        {
            redisConnection.getJedis();
            PlexLog.log("Connected to Redis!");
            MessageUtil.subscribe(this);

        }
        else
        {
            PlexLog.log("Redis is disabled in the configuration file, not connecting.");
        }

        punishmentRepository = new SQLPunishment(sqlConnection.getConnectionSource());
        playerRepository = new SQLPlayerData(sqlConnection.getConnectionSource(), punishmentRepository);
        noteRepository = new SQLNotes(sqlConnection.getConnectionSource());
        playerService = new PlayerService(playerCache, playerRepository);

        new ListenerHandler(this);
        new CommandHandler(this);

        punishmentManager = new PunishmentManager(this);
        punishmentManager.mergeIndefiniteBans();
        PlexLog.log("Punishment System initialized");

        if (!PlexUtils.isFolia())
        {
            // World generation is not supported on Folia yet
            generateWorlds();
        }

        serviceManager = new ServiceManager(this);
        PlexLog.log("Service Manager initialized");
        serviceManager.startServices();
        PlexLog.log("Started " + serviceManager.serviceCount() + " services.");

        reloadPlayers();
        PlexLog.debug("Registered Bukkit -> Proxy Plugin Messaging Channel");
        PlexLog.debug("Proxy enabled? " + Bukkit.getServerConfig().isProxyEnabled());
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        moduleManager.enableModules();
    }

    @Override
    public void onDisable()
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            PlexPlayer plexPlayer = playerCache.getPlexPlayerMap().get(player.getUniqueId()); //get the player because it's literally impossible for them to not have an object
            playerRepository.update(plexPlayer);
        });
        if (redisConnection != null && redisConnection.isEnabled())
        {
            PlexLog.log("Disabling Redis/Jedis. No memory leaks in this Anarchy server!");
        }

        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);

        moduleManager.disableModules();

        if (sqlConnection != null)
        {
            sqlConnection.close();
        }
        StorageExecutor.shutdown();
    }

    private void generateWorlds()
    {
        PlexLog.log("Generating any worlds if needed...");
        for (String key : config.getConfigurationSection("worlds").getKeys(false))
        {
            CustomWorld.generateConfigFlatWorld(this, key);
        }
        PlexLog.log("Finished with world generation!");
    }

    private void reloadPlayers()
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            PlexPlayer plexPlayer = playerService.getPlayer(player.getUniqueId());
            playerCache.getPlexPlayerMap().put(player.getUniqueId(), plexPlayer); //put them into the cache
        });
    }

    private Permission setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp != null)
        {
            permissions = rsp.getProvider();
        }
        return permissions;
    }

    private Chat setupChat()
    {
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp != null)
        {
            chat = rsp.getProvider();
        }
        return chat;
    }
}
