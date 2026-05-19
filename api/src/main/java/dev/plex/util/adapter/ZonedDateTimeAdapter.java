package dev.plex.util.adapter;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Gson adapter that stores {@link ZonedDateTime} values as epoch milliseconds in UTC.
 */
public class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime>
{
    private static final ZoneId UTC = ZoneId.of("Etc/UTC");

    /**
     * Creates a Gson adapter for {@link ZonedDateTime}.
     */
    public ZonedDateTimeAdapter()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
        return new JsonPrimitive(src.toInstant().toEpochMilli());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        Instant instant = Instant.ofEpochMilli(json.getAsJsonPrimitive().getAsLong());
        return ZonedDateTime.ofInstant(instant, UTC);
    }
}
