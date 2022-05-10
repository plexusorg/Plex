package dev.plex.toml;

import java.util.concurrent.atomic.AtomicInteger;

public class LiteralStringValueReader implements ValueReader
{
    public static final LiteralStringValueReader LITERAL_STRING_VALUE_READER = new LiteralStringValueReader();

    @Override
    public boolean canRead(String s)
    {
        return s.startsWith("'");
    }

    @Override
    public Object read(String s, AtomicInteger index, dev.plex.toml.Context context)
    {
        int startLine = context.line.get();
        boolean terminated = false;
        int startIndex = index.incrementAndGet();

        for (int i = index.get(); i < s.length(); i = index.incrementAndGet())
        {
            char c = s.charAt(i);

            if (c == '\'')
            {
                terminated = true;
                break;
            }
        }

        if (!terminated)
        {
            Results.Errors errors = new Results.Errors();
            errors.unterminated(context.identifier.getName(), s.substring(startIndex), startLine);
            return errors;
        }

        String substring = s.substring(startIndex, index.get());

        return substring;
    }

    private LiteralStringValueReader()
    {
    }
}
