package dev.plex.player;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerNameResolver
{
    private final PlayerService playerService;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();
    private final Map<UUID, String> profileCache = new ConcurrentHashMap<>();

    public PlayerNameResolver(PlayerService playerService)
    {
        this.playerService = playerService;
    }

    public String resolve(UUID uuid)
    {
        if (uuid == null)
        {
            return "CONSOLE";
        }

        Player online = Bukkit.getPlayer(uuid);
        if (online != null)
        {
            return online.getName();
        }

        String local = playerService.getNameByUUID(uuid);
        if (local != null && !local.isBlank())
        {
            return local;
        }

        String cached = profileCache.get(uuid);
        if (cached != null && !cached.isBlank())
        {
            return cached;
        }

        return lookupMojangName(uuid)
                .map(name ->
                {
                    profileCache.put(uuid, name);
                    return name;
                })
                .orElse(uuid.toString());
    }

    private Optional<String> lookupMojangName(UUID uuid)
    {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString().replace("-", "")))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        try
        {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
            {
                return Optional.empty();
            }
            JsonObject object = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!object.has("name") || !object.get("name").isJsonPrimitive())
            {
                return Optional.empty();
            }
            return Optional.ofNullable(object.get("name").getAsString()).filter(name -> !name.isBlank());
        }
        catch (IOException | InterruptedException | RuntimeException e)
        {
            if (e instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }
}
