package dev.plex.util.adapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import dev.plex.Plex;

import java.lang.reflect.Type;
import java.time.ZonedDateTime;

public class ZonedDateTimeSerializer implements JsonSerializer<ZonedDateTime>
{
    private static String TIMEZONE = Plex.get().config.getString("server.timezone");
    @Override
    public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
        return new JsonPrimitive(src.toInstant().toEpochMilli());
    }
}
