package dev.plex.updater;

import java.util.Locale;

public enum UpdateChannel
{
    STABLE("stable"),
    DEV("dev");

    private final String id;

    UpdateChannel(String id)
    {
        this.id = id;
    }

    public String id()
    {
        return id;
    }

    public static UpdateChannel fromConfig(String value)
    {
        if (value == null)
        {
            return STABLE;
        }

        return switch (value.trim().toLowerCase(Locale.ROOT))
        {
            case "dev" -> DEV;
            case "stable" -> STABLE;
            default -> STABLE;
        };
    }
}
