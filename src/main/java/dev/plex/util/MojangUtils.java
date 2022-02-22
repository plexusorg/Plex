package dev.plex.util;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class MojangUtils
{

    public static AshconInfo getInfo(String nameOrUuid)
    {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet("https://api.ashcon.app/mojang/v2/user/" + nameOrUuid);
        try
        {
            HttpResponse response = client.execute(get);
            if (response == null || response.getEntity() == null)
            {
                return null;
            }
            String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            JSONObject object = new JSONObject(json);
            if (!object.isNull("code") && object.getInt("code") == 404)
            {
                return null;
            }
            client.close();
            AshconInfo ashconInfo = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)(json1, typeOfT, context) ->
                    LocalDateTime.ofInstant(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(json1.getAsJsonPrimitive().getAsString())), ZoneId.systemDefault())).create().fromJson(json, AshconInfo.class);

            Arrays.sort(ashconInfo.getUsernameHistories(), (o1, o2) ->
            {
                if (o1.getLocalDateTime() == null || o2.getLocalDateTime() == null)
                {
                    return 1;
                }
                return o1.getLocalDateTime().compareTo(o2.getLocalDateTime());
            });

            return ashconInfo;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }
}
