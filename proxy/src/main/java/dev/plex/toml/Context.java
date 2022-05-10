package dev.plex.toml;

import java.util.concurrent.atomic.AtomicInteger;

public class Context
{
    final dev.plex.toml.Identifier identifier;
    final AtomicInteger line;
    final Results.Errors errors;

    public Context(dev.plex.toml.Identifier identifier, AtomicInteger line, Results.Errors errors)
    {
        this.identifier = identifier;
        this.line = line;
        this.errors = errors;
    }

    public Context with(dev.plex.toml.Identifier identifier)
    {
        return new Context(identifier, line, errors);
    }
}
