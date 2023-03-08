package dev.plex.settings;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;

import java.util.List;

@Getter
public class ServerSettings
{
    private final Server server = new Server();

    @Data
    public static class Server
    {
        private String name = "Server";
        private List<String> motd = Lists.newArrayList("%randomgradient%%servername% - %mcversion%", "Another motd");
        private boolean colorizeMotd = false;
        private boolean debug = false;
        private final List<String> sample = Lists.newArrayList("example", "example");
        @SerializedName(value = "add-player-count")
        private int addPlayerCount = 0;
        @SerializedName(value = "plus-one-max-count")
        private boolean plusOneMaxPlayer = true;
    }
}
