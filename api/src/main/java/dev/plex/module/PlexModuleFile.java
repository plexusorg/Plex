package dev.plex.module;

import java.util.List;

/**
 * Metadata read from a module's module.yml file.
 */
public final class PlexModuleFile
{
    private final String name;
    private final String main;
    private final String description;
    private final String version;
    private final int apiCompatibility;
    private List<String> libraries = List.of();

    public PlexModuleFile(String name, String main, String description, String version, int apiCompatibility)
    {
        this.name = name;
        this.main = main;
        this.description = description;
        this.version = version;
        this.apiCompatibility = apiCompatibility;
    }

    public String getName()
    {
        return name;
    }

    public String getMain()
    {
        return main;
    }

    public String getDescription()
    {
        return description;
    }

    public String getVersion()
    {
        return version;
    }

    public int getApiCompatibility()
    {
        return apiCompatibility;
    }

    public List<String> getLibraries()
    {
        return libraries;
    }

    public void setLibraries(List<String> libraries)
    {
        this.libraries = List.copyOf(libraries);
    }
}
