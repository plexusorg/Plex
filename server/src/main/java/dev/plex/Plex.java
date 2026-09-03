package dev.plex;

import io.papermc.paper.ServerBuildInfo;

import org.bukkit.Bukkit;

import dev.plex.api.PlexApi;
import dev.plex.api.impl.DefaultPlexApi;
import dev.plex.command.PlexCommand;
import dev.plex.command.ServerCommand;
import dev.plex.config.Config;
import dev.plex.handlers.CommandHandler;
import dev.plex.handlers.ListenerHandler;
import dev.plex.hook.CoreProtectHook;
import dev.plex.hook.PrismHook;
import dev.plex.hook.WorldGuardHook;
import dev.plex.module.ModuleManager;
import dev.plex.network.ProxyVanishBridge;
import dev.plex.note.NotesService;
import dev.plex.player.PlayerService;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.PunishmentManager;
import dev.plex.services.ServiceManager;
import dev.plex.storage.RedisConnection;
import dev.plex.storage.StorageType;
import dev.plex.storage.database.Database;
import dev.plex.storage.player.SQLPlayerData;
import dev.plex.storage.player.PlayerModuleDataRepository;
import dev.plex.storage.player.SQLPlayerModuleData;
import dev.plex.storage.punishment.SQLNotes;
import dev.plex.storage.punishment.SQLPunishment;
import dev.plex.storage.repository.NoteRepository;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.updater.UpdateChannel;
import dev.plex.util.BuildInfo;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.UpdateChecker;
import dev.plex.util.redis.MessageUtil;
import dev.plex.world.CustomWorld;
import dev.plex.world.WorldSpawnSignManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.key.Key;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.ConfigurationSection;
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
    public Config entities;
    public Config worlds;
    public File modulesFolder;
    private boolean migrateLegacyEntityConfig;
    private boolean migrateLegacyWorldConfig;
    private StorageType storageType = StorageType.SQLITE;
    private Database database;
    private ThreadPoolExecutor databaseExecutor;
    private final ExecutorService ioExecutor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual().name("Plex-IO-", 0).factory());
    private RedisConnection redisConnection;

    private PlayerModuleDataRepository playerModuleDataRepository;
    private PlayerService playerService;
    private ProxyVanishBridge proxyVanishBridge;

    private PunishmentRepository punishmentRepository;
    private NoteRepository noteRepository;

    private ModuleManager moduleManager;
    private CommandHandler commandHandler;
    private final List<PlexCommand> pendingCommands = new ArrayList<>();
    private ServiceManager serviceManager;
    private PunishmentManager punishmentManager;
    private UpdateChecker updateChecker;
    private NotesService notesService;
    private PlexApi api;
    private WorldSpawnSignManager worldSpawnSignManager;

    private Permission permissions;
    private Chat chat;

    private CoreProtectHook coreProtectHook;
    private PrismHook prismHook;
    private WorldGuardHook worldGuardHook;

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
        migrateLegacyEntityConfig = !new File(getDataFolder(), "entities.yml").exists();
        migrateLegacyWorldConfig = !new File(getDataFolder(), "worlds.yml").exists();
        entities = new Config(this, "entities.yml");
        worlds = new Config(this, "worlds.yml");
        build.load(this);
        notesService = new NotesService(this);
        moduleManager = new ModuleManager(this);
        api = new DefaultPlexApi(this, MODULE_API_COMPATIBILITY_VERSION, notesService);
        installModuleApiRuntimes();

        modulesFolder = new File(this.getDataFolder() + File.separator + "modules");
        if (!modulesFolder.exists())
        {
            modulesFolder.mkdir();
        }

        moduleManager.load();

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
        });
    }

    @Override
    public void onEnable()
    {
        config.load();
        entities.load();
        worlds.load();
        migrateSplitConfigs();
        updateConfiguredChannel();
        PlexLog.setDebugEnabled(config.getBoolean("debug"));
        messages.load();
        PlexUtils.configure(config, messages);
        TimeUtils.TIMEZONE = config.getString("server.timezone", "Etc/UTC");
        toggles.load();

        // Don't add default entries to these files
        indefBans.load(false);

        database = new Database(this);
        databaseExecutor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(512), databaseThreads(), new ThreadPoolExecutor.AbortPolicy());
        redisConnection = new RedisConnection(this);

        PlexLog.log("Attempting to connect to DB: {0}", plugin.config.getString("data.db.name"));
        PlexLog.log("Connected to " + storageType.name().toUpperCase());

        if (!getServer().getPluginManager().isPluginEnabled("Vault"))
        {
            throw new RuntimeException("Vault is required to run on the server alongside a permissions plugin, we recommend LuckPerms!");
        }

        permissions = setupPermissions();
        chat = setupChat();

        if (plugin.getServer().getPluginManager().isPluginEnabled("CoreProtect"))
        {
            coreProtectHook = new CoreProtectHook(this);
            PlexLog.log("CoreProtect API available: {0}", coreProtectHook.hasCoreProtect());
        }
        else
        {
            PlexLog.debug("Not hooking into CoreProtect");
        }
        if (plugin.getServer().getPluginManager().isPluginEnabled("prism"))
        {
            prismHook = new PrismHook(this);
            PlexLog.log("Prism API available: {0}", prismHook.hasPrism());
        }
        else
        {
            PlexLog.debug("Not hooking into Prism");
        }

        PlexLog.log("SuperVanish / PremiumVanish available: {0}", PlexUtils.hasVanishPlugin());

        if (plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard"))
        {
            try
            {
                worldGuardHook = new WorldGuardHook(this);
                PlexLog.log("Hooked into WorldGuard!");
            }
            catch (LinkageError | RuntimeException ex)
            {
                worldGuardHook = null;
                PlexLog.warn("WorldGuard was found, but its API was unavailable. The protect command will not be registered.");
                PlexLog.debug("WorldGuard hook failure: {0}", ex.getMessage());
            }
        }
        else
        {
            PlexLog.debug("Not hooking into WorldGuard");
        }
        updateChecker = new UpdateChecker(this);
        PlexLog.log("Update checking enabled");

        // https://bstats.org/plugin/bukkit/Plex/14143
        Metrics metrics = new Metrics(this, 14143);
        PlexLog.log("Enabled Metrics");

        if (redisConnection != null && redisConnection.isEnabled())
        {
            try
            {
                redisConnection.ping();
                PlexLog.log("Connected to Redis!");
            }
            catch (RuntimeException ex)
            {
                PlexLog.warn("Redis is unavailable; messaging will reconnect in the background: {0}", ex.getMessage());
            }
            MessageUtil.subscribe(this);

        }
        else
        {
            PlexLog.log("Redis is disabled in the configuration file, not connecting.");
        }

        punishmentRepository = new SQLPunishment(database.getJdbi(), databaseExecutor);
        PlayerRepository playerRepository = new SQLPlayerData(database.getJdbi(), punishmentRepository, storageType);
        playerModuleDataRepository = new SQLPlayerModuleData(database.getJdbi(), storageType);
        noteRepository = new SQLNotes(database.getJdbi(), databaseExecutor);
        playerService = new PlayerService(playerRepository, databaseExecutor);
        proxyVanishBridge = new ProxyVanishBridge(this);

        worldSpawnSignManager = new WorldSpawnSignManager(this);
        new ListenerHandler(this);
        commandHandler = new CommandHandler(this);

        punishmentManager = new PunishmentManager(this);
        MessageUtil.onBanInvalidation(
                invalidation -> punishmentManager.handleBanInvalidation(invalidation.playerId(), invalidation.ip()));
        punishmentManager.mergeIndefiniteBans();
        PlexLog.log("Punishment System initialized");

        if (!PlexUtils.isFolia())
        {
            // World generation is not supported on Folia yet
            generateWorlds();
        }
        worldSpawnSignManager.start();

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

    private void updateConfiguredChannel()
    {
        String channel = UpdateChannel.forVersion(getPluginMeta().getVersion()).id();
        if (!channel.equals(config.getString("updater.channel")))
        {
            config.set("updater.channel", channel);
            config.save();
        }
    }

    @Override
    public void onDisable()
    {
        if (redisConnection != null && redisConnection.isEnabled())
        {
            PlexLog.log("Disabling Redis/Jedis. No memory leaks in this Anarchy server!");
        }
        MessageUtil.close();
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);

        if (worldSpawnSignManager != null)
        {
            worldSpawnSignManager.stop();
        }

        if (serviceManager != null)
        {
            serviceManager.shutdownServices();
        }

        try
        {
            moduleManager.unloadModulesDuringServerShutdown().get(10, TimeUnit.SECONDS);
        }
        catch (TimeoutException failure)
        {
            PlexLog.error("Timed out waiting for modules to finish shutting down", failure);
        }
        catch (ExecutionException failure)
        {
            PlexLog.error("A module failed to finish shutting down", failure.getCause());
        }
        catch (InterruptedException failure)
        {
            Thread.currentThread().interrupt();
            PlexLog.error("Interrupted while waiting for modules to finish shutting down", failure);
        }

        if (playerService != null)
        {
            try
            {
                playerService.flush().orTimeout(10, TimeUnit.SECONDS).join();
            }
            catch (RuntimeException failure)
            {
                PlexLog.error("Unable to flush all player sessions: {0}", failure.getMessage());
            }
        }
        if (redisConnection != null)
        {
            redisConnection.close();
        }

        ioExecutor.shutdownNow();

        if (databaseExecutor != null)
        {
            databaseExecutor.shutdown();
            try
            {
                if (!databaseExecutor.awaitTermination(10, TimeUnit.SECONDS)) databaseExecutor.shutdownNow();
            }
            catch (InterruptedException failure)
            {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        if (database != null)
        {
            database.close();
        }
    }

    private void generateWorlds()
    {
        PlexLog.log("Generating any worlds if needed...");
        for (String key : worlds.getConfigurationSection("worlds").getKeys(false))
        {
            CustomWorld.generateConfigFlatWorld(this, key);
        }
        PlexLog.log("Finished with world generation!");
    }

    private void migrateSplitConfigs()
    {
        boolean configChanged = false;
        if (migrateLegacyEntityConfig)
        {
            boolean entitiesChanged = migrateConfigRoots(config, entities, List.of(
                    "entitywipe_list", "autowipe", "blocked_blocks", "blocked_entities", "entity_limit"));
            configChanged |= entitiesChanged;
            if (entitiesChanged)
            {
                entities.save();
                PlexLog.log("Moved entity settings from config.yml to entities.yml.");
            }
        }

        boolean worldsChanged = false;
        if (migrateLegacyWorldConfig)
        {
            worldsChanged = migrateConfigRoots(config, worlds, List.of("global_gamerules", "worlds"));
            configChanged |= worldsChanged;
            if (worldsChanged)
            {
                worlds.save();
                PlexLog.log("Moved world settings from config.yml to worlds.yml.");
            }
        }

        if (configChanged)
        {
            config.save();
        }
        migrateLegacyEntityConfig = false;
        migrateLegacyWorldConfig = false;
    }

    private boolean migrateConfigRoots(Config source, Config target, List<String> roots)
    {
        boolean changed = false;
        for (String root : roots)
        {
            if (!source.contains(root))
            {
                continue;
            }
            ConfigurationSection section = source.getConfigurationSection(root);
            if (section == null)
            {
                target.set(root, source.get(root));
            }
            else
            {
                for (String path : section.getKeys(true))
                {
                    if (!section.isConfigurationSection(path))
                    {
                        target.set(root + "." + path, section.get(path));
                    }
                }
            }
            source.set(root, null);
            changed = true;
        }
        return changed;
    }

    private void reloadPlayers()
    {
        Bukkit.getOnlinePlayers().forEach(player ->
        {
            UUID playerId = player.getUniqueId();
            PlexPlayer expected = playerService.cachedPlayer(playerId);
            String ip = player.getAddress() == null ? "" : player.getAddress().getAddress().getHostAddress();
            punishmentManager.trackOnlineCapacity(player, ip);
            playerService.reloadSession(playerId, player.getName(), ip).whenComplete((plexPlayer, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.warn("Unable to reload player {0}: {1}", playerId, failure.getMessage());
                    return;
                }
                player.getScheduler().run(this, task ->
                {
                    if (player.isOnline() && playerService.attachReloadedSession(playerId, expected, plexPlayer))
                    {
                        punishmentManager.restoreTimedState(plexPlayer);
                        punishmentManager.trackReloadedPlayer(player, ip);
                    }
                }, null);
            });
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

    private static ThreadFactory databaseThreads()
    {
        return Thread.ofPlatform().daemon().name("Plex-Database-", 0).factory();
    }
}
