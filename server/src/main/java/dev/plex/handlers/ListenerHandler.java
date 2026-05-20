package dev.plex.handlers;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.listener.impl.AntiNukerListener;
import dev.plex.listener.impl.AntiSpamListener;
import dev.plex.listener.impl.BanListener;
import dev.plex.listener.impl.BlockListener;
import dev.plex.listener.impl.BookListener;
import dev.plex.listener.impl.ChatListener;
import dev.plex.listener.impl.CommandListener;
import dev.plex.listener.impl.DropListener;
import dev.plex.listener.impl.FreezeListener;
import dev.plex.listener.impl.GameModeListener;
import dev.plex.listener.impl.MenuListener;
import dev.plex.listener.impl.MobListener;
import dev.plex.listener.impl.MuteListener;
import dev.plex.listener.impl.PlayerListener;
import dev.plex.listener.impl.ServerListener;
import dev.plex.listener.impl.TabListener;
import dev.plex.listener.impl.TogglesListener;
import dev.plex.listener.impl.VanishListener;
import dev.plex.listener.impl.WorldListener;
import dev.plex.util.PlexLog;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ListenerHandler
{
    private final Plex plugin;
    private final List<ServerListenerBase> listeners = new ArrayList<>();

    public ListenerHandler(Plex plugin)
    {
        this.plugin = plugin;
        registerBuiltInListeners();
        PlexLog.log("Registered " + listeners.size() + " listeners.");
    }

    private void registerBuiltInListeners()
    {
        register(() -> new AntiNukerListener(plugin));
        register(() -> new AntiSpamListener(plugin));
        register(() -> new BanListener(plugin));
        register(() -> new BlockListener(plugin));
        register(() -> new BookListener(plugin));
        registerIfEnabled("chat.enabled", () -> new ChatListener(plugin));
        register(() -> new CommandListener(plugin));
        register(() -> new DropListener(plugin));
        register(() -> new FreezeListener(plugin));
        register(() -> new GameModeListener(plugin));
        register(() -> new MenuListener(plugin));
        register(() -> new MobListener(plugin));
        register(() -> new MuteListener(plugin));
        register(() -> new PlayerListener(plugin));
        register(() -> new ServerListener(plugin));
        register(() -> new TabListener(plugin));
        register(() -> new TogglesListener(plugin));
        register(() -> new VanishListener(plugin));
        register(() -> new WorldListener(plugin));
    }

    private void register(Supplier<ServerListenerBase> listener)
    {
        listeners.add(listener.get());
    }

    private void registerIfEnabled(String configPath, Supplier<ServerListenerBase> listener)
    {
        if (plugin.config.get(configPath) != null && plugin.config.getBoolean(configPath))
        {
            register(listener);
        }
    }
}
