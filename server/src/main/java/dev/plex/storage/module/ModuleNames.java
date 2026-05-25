package dev.plex.storage.module;

import dev.plex.module.PlexModule;

import java.util.Locale;

public final class ModuleNames
{
    private static final int MAX_PREFIX_LENGTH = 40;

    private ModuleNames()
    {
    }

    public static String prefix(PlexModule module)
    {
        String name = module.getPlexModuleFile().getName().toLowerCase(Locale.ROOT);
        if (name.startsWith("module-"))
        {
            name = name.substring("module-".length());
        }
        name = name.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
        if (name.isBlank())
        {
            throw new IllegalArgumentException("Module name does not produce a valid storage prefix");
        }
        if (name.length() > MAX_PREFIX_LENGTH)
        {
            name = name.substring(0, MAX_PREFIX_LENGTH).replaceAll("_+$", "");
        }
        return name;
    }

    public static String table(String prefix, String localName)
    {
        return prefix + "_" + validateLocalName(localName);
    }

    public static String validateLocalName(String localName)
    {
        if (localName == null || !localName.matches("[a-z][a-z0-9_]{0,47}"))
        {
            throw new IllegalArgumentException("Invalid module table name: " + localName);
        }
        return localName;
    }
}
