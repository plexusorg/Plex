package dev.plex.util;

import com.google.common.collect.Maps;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class MojangUtils
{
    public static UUID getUUID(String name)
    {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("https://api.mojang.com/users/profiles/minecraft/" + name);
        try
        {
            HttpResponse response = client.execute(get);
            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject object = new JSONObject(json);
            client.close();
            return UUID.fromString(new StringBuilder(object.getString("id"))
                    .insert(8, "-")
                    .insert(13, "-")
                    .insert(18, "-")
                    .insert(23, "-").toString());
        } catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static List<Map.Entry<String, LocalDateTime>> getNameHistory(UUID uuid)
    {
        Map<String, LocalDateTime> names = Maps.newHashMap();
        String uuidString = uuid.toString().replace("-", "");
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet("https://api.mojang.com/user/profiles/" + uuidString + "/names");
        try
        {
            HttpResponse response = httpClient.execute(get);
            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);
            array.forEach(object ->
            {
                JSONObject obj = new JSONObject(object.toString());
                String name = obj.getString("name");
                if (!obj.isNull("changedToAt"))
                {
                    long dateTime = obj.getLong("changedToAt");
                    Instant instant = Instant.ofEpochMilli(dateTime);
                    LocalDateTime time = LocalDateTime.ofInstant(instant, ZoneId.of("America/Los_Angeles"));
                    names.put(name, time);
                } else
                {
                    names.put(name, null);
                }
            });
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        return names.entrySet().stream().sorted(Map.Entry.comparingByValue()).collect(Collectors.toList());
    }
}
