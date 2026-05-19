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
    private List<String> repositories = List.of();

    /**
     * Creates module metadata.
     *
     * @param name module name
     * @param main main module class
     * @param description module description
     * @param version module version
     * @param apiCompatibility required Plex API compatibility version
     */
    public PlexModuleFile(String name, String main, String description, String version, int apiCompatibility)
    {
        this.name = name;
        this.main = main;
        this.description = description;
        this.version = version;
        this.apiCompatibility = apiCompatibility;
    }

    /**
     * Returns the module name.
     *
     * @return module name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the main module class.
     *
     * @return main module class
     */
    public String getMain()
    {
        return main;
    }

    /**
     * Returns the module description.
     *
     * @return module description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Returns the module version.
     *
     * @return module version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Returns the required Plex API compatibility version.
     *
     * @return required Plex API compatibility version
     */
    public int getApiCompatibility()
    {
        return apiCompatibility;
    }

    /**
     * Returns dependency libraries declared by the module.
     *
     * @return dependency libraries declared by the module
     */
    public List<String> getLibraries()
    {
        return libraries;
    }

    /**
     * Sets dependency libraries declared by the module.
     *
     * @param libraries dependency libraries
     */
    public void setLibraries(List<String> libraries)
    {
        this.libraries = List.copyOf(libraries);
    }

    /**
     * Returns Maven repositories declared by the module.
     *
     * @return Maven repositories declared by the module
     */
    public List<String> getRepositories()
    {
        return repositories;
    }

    /**
     * Sets Maven repositories declared by the module.
     *
     * @param repositories Maven repositories
     */
    public void setRepositories(List<String> repositories)
    {
        this.repositories = List.copyOf(repositories);
    }
}
