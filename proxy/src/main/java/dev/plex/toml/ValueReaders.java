package dev.plex.toml;

import java.util.concurrent.atomic.AtomicInteger;

import static dev.plex.toml.ArrayValueReader.ARRAY_VALUE_READER;
import static dev.plex.toml.BooleanValueReaderWriter.BOOLEAN_VALUE_READER_WRITER;
import static dev.plex.toml.DateValueReaderWriter.DATE_VALUE_READER_WRITER;
import static dev.plex.toml.LiteralStringValueReader.LITERAL_STRING_VALUE_READER;
import static dev.plex.toml.MultilineLiteralStringValueReader.MULTILINE_LITERAL_STRING_VALUE_READER;
import static dev.plex.toml.MultilineStringValueReader.MULTILINE_STRING_VALUE_READER;
import static dev.plex.toml.StringValueReaderWriter.STRING_VALUE_READER_WRITER;

class ValueReaders
{

    static final ValueReaders VALUE_READERS = new ValueReaders();

    Object convert(String value, AtomicInteger index, dev.plex.toml.Context context)
    {
        String substring = value.substring(index.get());
        for (dev.plex.toml.ValueReader valueParser : READERS)
        {
            if (valueParser.canRead(substring))
            {
                return valueParser.read(value, index, context);
            }
        }

        dev.plex.toml.Results.Errors errors = new dev.plex.toml.Results.Errors();
        errors.invalidValue(context.identifier.getName(), substring, context.line.get());
        return errors;
    }

    private ValueReaders()
    {
    }

    private static final dev.plex.toml.ValueReader[] READERS = {
            MULTILINE_STRING_VALUE_READER, MULTILINE_LITERAL_STRING_VALUE_READER, LITERAL_STRING_VALUE_READER, STRING_VALUE_READER_WRITER, DATE_VALUE_READER_WRITER, NumberValueReaderWriter.NUMBER_VALUE_READER_WRITER, BOOLEAN_VALUE_READER_WRITER, ARRAY_VALUE_READER, InlineTableValueReader.INLINE_TABLE_VALUE_READER
    };
}
