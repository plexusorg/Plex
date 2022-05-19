package dev.plex.toml;


import java.io.File;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class MapValueWriter implements dev.plex.toml.ValueWriter
{
    static final dev.plex.toml.ValueWriter MAP_VALUE_WRITER = new MapValueWriter();

    private static final Pattern REQUIRED_QUOTING_PATTERN = Pattern.compile("^.*[^A-Za-z\\d_-].*$");

    @Override
    public boolean canWrite(Object value)
    {
        return value instanceof Map;
    }

    @Override
    public void write(Object value, WriterContext context)
    {
        File file = null;
        if (context.file != null)
        {
            file = context.file;
        }

        Map<?, ?> from = (Map<?, ?>) value;

        Toml toml = null;

        if (file != null)
        {
            toml = new Toml().read(file);
        }

        if (hasPrimitiveValues(from, context))
        {
            if (context.hasRun)
            {
                if (toml != null)
                {
                    if (!toml.getValues().containsKey(context.key))
                    {
                        context.writeKey();
                    }
                }
                else
                {
                    context.writeKey();
                }
            }

        }


        // Render primitive types and arrays of primitive first so they are
        // grouped under the same table (if there is one)
        for (Map.Entry<?, ?> entry : from.entrySet())
        {
            Object key = entry.getKey();
            Object fromValue = entry.getValue();
            if (fromValue == null)
            {
                continue;
            }

            if (context.hasRun && toml != null)
            {
                if (context.key != null)
                {
                    if (key.toString().equalsIgnoreCase(context.key))
                    {
                        continue;
                    }
                    if (toml.contains(context.key + "." + key))
                    {
                        continue;
                    }
                }
            }

            dev.plex.toml.ValueWriter valueWriter = dev.plex.toml.ValueWriters.WRITERS.findWriterFor(fromValue);
            if (valueWriter.isPrimitiveType())
            {
                context.indent();
                context.write(quoteKey(key)).write(" = ");
                valueWriter.write(fromValue, context);
                context.write('\n');
            }
            else if (valueWriter == dev.plex.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER)
            {
                context.indent();
                context.setArrayKey(key.toString());
                context.write(quoteKey(key)).write(" = ");
                valueWriter.write(fromValue, context);
                context.write('\n');
            }
        }

        // Now render (sub)tables and arrays of tables
        for (Object key : from.keySet())
        {
            Object fromValue = from.get(key);
            if (fromValue == null)
            {
                continue;
            }

            if (context.hasRun && toml != null)
            {
                if (context.key != null)
                {
                    if (key.toString().equalsIgnoreCase(context.key))
                    {
                        continue;
                    }
                    if (toml.contains(context.key + "." + key))
                    {
                        continue;
                    }
                }
            }

            dev.plex.toml.ValueWriter valueWriter = dev.plex.toml.ValueWriters.WRITERS.findWriterFor(fromValue);
            if (valueWriter == this || valueWriter == dev.plex.toml.ObjectValueWriter.OBJECT_VALUE_WRITER || valueWriter == dev.plex.toml.TableArrayValueWriter.TABLE_ARRAY_VALUE_WRITER)
            {
                WriterContext context1 = context.pushTable(quoteKey(key));
                context1.parentName = key.toString();
                context1.hasRun = true;
                context1.file = context.file;
                valueWriter.write(fromValue, context1);
            }
        }
    }

    @Override
    public boolean isPrimitiveType()
    {
        return false;
    }

    private static String quoteKey(Object key)
    {
        String stringKey = key.toString();
        Matcher matcher = REQUIRED_QUOTING_PATTERN.matcher(stringKey);
        if (matcher.matches())
        {
            stringKey = "\"" + stringKey + "\"";
        }

        return stringKey;
    }

    private static boolean hasPrimitiveValues(Map<?, ?> values, WriterContext context)
    {
        for (Object key : values.keySet())
        {
            Object fromValue = values.get(key);
            if (fromValue == null)
            {
                continue;
            }

            dev.plex.toml.ValueWriter valueWriter = dev.plex.toml.ValueWriters.WRITERS.findWriterFor(fromValue);
            if (valueWriter.isPrimitiveType() || valueWriter == dev.plex.toml.PrimitiveArrayValueWriter.PRIMITIVE_ARRAY_VALUE_WRITER)
            {
                return true;
            }
        }

        return false;
    }


    private MapValueWriter()
    {
    }
}
