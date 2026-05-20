package dev.plex.listener.impl;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.server.ServerPing;
import dev.plex.Plex;
import dev.plex.listener.ProxyListener;
import dev.plex.settings.ServerSettings;
import dev.plex.util.RandomUtil;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ServerListener extends ProxyListener
{
    public ServerListener(Plex plugin)
    {
        super(plugin);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPing(ProxyPingEvent event)
    {
        ServerSettings.Server config = plugin.getConfig().settings().getServer();
        List<String> motds = config.getMotd();
        String baseMotd = motds.get(ThreadLocalRandom.current().nextInt(motds.size()));
        baseMotd = baseMotd.replace("\\n", "\n");
        baseMotd = baseMotd.replace("%servername%", config.getName());
        baseMotd = baseMotd.replace("%mcversion%", plugin.getServer().getVersion().getVersion().split(" ")[0]);
        baseMotd = baseMotd.replace("%randomgradient%", "<gradient:" + RandomUtil.getRandomColor().toString() + ":" + RandomUtil.getRandomColor().toString() + ">");

        ServerPing.Builder builder = event.getPing().asBuilder();

        if (config.isColorizeMotd())
        {
            AtomicReference<Component> motd = new AtomicReference<>(Component.empty());
            for (final String word : baseMotd.split(" "))
            {
                motd.set(motd.get().append(Component.text(word).color(RandomUtil.getRandomColor())));
                motd.set(motd.get().append(Component.space()));
            }
            builder.description(motd.get());
        }
        else
        {
            builder.description(MiniMessage.miniMessage().deserialize(baseMotd));
        }

        builder.samplePlayers(config.getSample().stream().map(s -> new ServerPing.SamplePlayer(convertColorCodes(s), UUID.randomUUID())).toArray(ServerPing.SamplePlayer[]::new));
        builder.onlinePlayers(plugin.getServer().getPlayerCount() + config.getAddPlayerCount());
        if (config.isPlusOneMaxPlayer())
        {
            builder.maximumPlayers(builder.getOnlinePlayers() + 1);
        }

        event.setPing(builder.build());

    }

    private String convertColorCodes(String code)
    {
        Matcher matcher = Pattern.compile("[&][0-9a-fk-or]{1}").matcher(code);
        return matcher.replaceAll(matchResult -> "§" + matcher.group().substring(1));
    }
}
