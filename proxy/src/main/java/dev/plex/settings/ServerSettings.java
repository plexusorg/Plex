package dev.plex.settings;

import dev.plex.api.config.PlexConfiguration;
import java.util.List;
import lombok.Getter;

@Getter
public class ServerSettings
{
    private static final List<String> DEFAULT_MOTD = List.of("%randomgradient%%servername% - %mcversion%", "Another motd");
    private static final List<String> DEFAULT_SAMPLE = List.of("example", "example");

    private final Server server;

    public ServerSettings(PlexConfiguration config)
    {
        this.server = new Server(config);
    }

    @Getter
    public static class Server
    {
        private final String name;
        private final List<String> motd;
        private final boolean colorizeMotd;
        private final boolean debug;
        private final List<String> sample;
        private final int addPlayerCount;
        private final boolean plusOneMaxPlayer;

        private Server(PlexConfiguration config)
        {
            this.name = config.getString("server.name", "Plexus");
            this.motd = config.getStringList("server.motd", DEFAULT_MOTD);
            this.colorizeMotd = config.getBoolean("server.colorize_motd", false);
            this.debug = config.getBoolean("server.debug", false);
            this.sample = config.getStringList("server.sample", DEFAULT_SAMPLE);
            this.addPlayerCount = config.getInt("server.add_player_count", 0);
            this.plusOneMaxPlayer = config.getBoolean("server.plus_one_max_count", true);
        }
    }
}
