package dev.plex.settings;

import com.google.common.collect.Lists;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
public class ServerSettings
{
    private final Server server = new Server();

    @Data
    public static class Server {
        private String name = "Plexus";
        private String motd = "Test MOTD";
        private boolean colorizeMotd = false;
        private boolean debug = false;
        private final List<String> sample = Lists.newArrayList("example", "example");
    }
}
