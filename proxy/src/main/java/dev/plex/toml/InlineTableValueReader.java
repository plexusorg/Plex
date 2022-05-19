package dev.plex.toml;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

class InlineTableValueReader implements dev.plex.toml.ValueReader
{

    static final InlineTableValueReader INLINE_TABLE_VALUE_READER = new InlineTableValueReader();

    @Override
    public boolean canRead(String s)
    {
        return s.startsWith("{");
    }

    @Override
    public Object read(String s, AtomicInteger sharedIndex, dev.plex.toml.Context context)
    {
        AtomicInteger line = context.line;
        int startLine = line.get();
        int startIndex = sharedIndex.get();
        boolean inKey = true;
        boolean inValue = false;
        boolean terminated = false;
        StringBuilder currentKey = new StringBuilder();
        HashMap<String, Object> results = new HashMap<String, Object>();
        dev.plex.toml.Results.Errors errors = new dev.plex.toml.Results.Errors();

        for (int i = sharedIndex.incrementAndGet(); sharedIndex.get() < s.length(); i = sharedIndex.incrementAndGet())
        {
            char c = s.charAt(i);

            if (inValue && !Character.isWhitespace(c))
            {
                Object converted = dev.plex.toml.ValueReaders.VALUE_READERS.convert(s, sharedIndex, context.with(dev.plex.toml.Identifier.from(currentKey.toString(), context)));

                if (converted instanceof dev.plex.toml.Results.Errors)
                {
                    errors.add((dev.plex.toml.Results.Errors) converted);
                    return errors;
                }

                String currentKeyTrimmed = currentKey.toString().trim();
                Object previous = results.put(currentKeyTrimmed, converted);

                if (previous != null)
                {
                    errors.duplicateKey(currentKeyTrimmed, context.line.get());
                    return errors;
                }

                currentKey = new StringBuilder();
                inValue = false;
            }
            else if (c == ',')
            {
                inKey = true;
                inValue = false;
                currentKey = new StringBuilder();
            }
            else if (c == '=')
            {
                inKey = false;
                inValue = true;
            }
            else if (c == '}')
            {
                terminated = true;
                break;
            }
            else if (inKey)
            {
                currentKey.append(c);
            }
        }

        if (!terminated)
        {
            errors.unterminated(context.identifier.getName(), s.substring(startIndex), startLine);
        }

        if (errors.hasErrors())
        {
            return errors;
        }

        return results;
    }

    private InlineTableValueReader()
    {
    }
}
