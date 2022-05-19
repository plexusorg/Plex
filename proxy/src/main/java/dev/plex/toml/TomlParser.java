package dev.plex.toml;

import java.util.concurrent.atomic.AtomicInteger;

class TomlParser
{

    static dev.plex.toml.Results run(String tomlString)
    {
        final dev.plex.toml.Results results = new dev.plex.toml.Results();

        if (tomlString.isEmpty())
        {
            return results;
        }

        AtomicInteger index = new AtomicInteger();
        boolean inComment = false;
        AtomicInteger line = new AtomicInteger(1);
        dev.plex.toml.Identifier identifier = null;
        Object value = null;

        for (int i = index.get(); i < tomlString.length(); i = index.incrementAndGet())
        {
            char c = tomlString.charAt(i);

            if (results.errors.hasErrors())
            {
                break;
            }

            if (c == '#' && !inComment)
            {
                inComment = true;
            }
            else if (!Character.isWhitespace(c) && !inComment && identifier == null)
            {
                dev.plex.toml.Identifier id = dev.plex.toml.IdentifierConverter.IDENTIFIER_CONVERTER.convert(tomlString, index, new dev.plex.toml.Context(null, line, results.errors));

                if (id != dev.plex.toml.Identifier.INVALID)
                {
                    if (id.isKey())
                    {
                        identifier = id;
                    }
                    else if (id.isTable())
                    {
                        results.startTables(id, line);
                    }
                    else if (id.isTableArray())
                    {
                        results.startTableArray(id, line);
                    }
                }
            }
            else if (c == '\n')
            {
                inComment = false;
                identifier = null;
                value = null;
                line.incrementAndGet();
            }
            else if (!inComment && identifier != null && identifier.isKey() && value == null && !Character.isWhitespace(c))
            {
                value = ValueReaders.VALUE_READERS.convert(tomlString, index, new dev.plex.toml.Context(identifier, line, results.errors));

                if (value instanceof dev.plex.toml.Results.Errors)
                {
                    results.errors.add((dev.plex.toml.Results.Errors) value);
                }
                else
                {
                    results.addValue(identifier.getName(), value, line);
                }
            }
            else if (value != null && !inComment && !Character.isWhitespace(c))
            {
                results.errors.invalidTextAfterIdentifier(identifier, c, line.get());
            }
        }

        return results;
    }

    private TomlParser()
    {
    }
}
