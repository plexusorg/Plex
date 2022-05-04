package dev.plex.listener.impl;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import dev.plex.Plex;
import dev.plex.listener.PlexListener;
import dev.plex.settings.ServerSettings;
import dev.plex.util.PlexLog;
import dev.plex.util.RandomUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.concurrent.atomic.AtomicReference;

public class ServerListener extends PlexListener
{
    @Subscribe(order = PostOrder.FIRST)
    public void onPing(ProxyPingEvent event)
    {
        String baseMotd = plugin.getConfig().as(ServerSettings.class).getServer().getMotd();
        baseMotd = baseMotd.replace("\\n", "\n");
        baseMotd = baseMotd.replace("%servername%", plugin.getConfig().as(ServerSettings.class).getServer().getName());
        baseMotd = baseMotd.replace("%mcversion%", plugin.getServer().getVersion().getVersion());

        PlexLog.log(baseMotd);

        if (plugin.getConfig().as(ServerSettings.class).getServer().isColorizeMotd())
        {
            AtomicReference<Component> motd = new AtomicReference<>(Component.empty());
            for (final String word : baseMotd.split(" "))
            {
                motd.set(motd.get().append(Component.text(word).color(RandomUtil.getRandomColor())));
                motd.set(motd.get().append(Component.space()));
            }
            event.setPing(event.getPing().asBuilder().description(motd.get()).build());
        } else {
            event.setPing(event.getPing().asBuilder().description(MiniMessage.miniMessage().deserialize(baseMotd)).build());
        }
    }

}
