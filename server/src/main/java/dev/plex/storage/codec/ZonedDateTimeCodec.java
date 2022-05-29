package dev.plex.storage.codec;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static dev.plex.util.TimeUtils.TIMEZONE;

public class ZonedDateTimeCodec implements Codec<ZonedDateTime>
{
    @Override
    public ZonedDateTime decode(BsonReader reader, DecoderContext decoderContext)
    {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(reader.readDateTime()), ZoneId.of(TIMEZONE));
    }

    @Override
    public void encode(BsonWriter writer, ZonedDateTime value, EncoderContext encoderContext)
    {
        writer.writeDateTime(value.toInstant().toEpochMilli());
    }

    @Override
    public Class<ZonedDateTime> getEncoderClass()
    {
        return ZonedDateTime.class;
    }
}
