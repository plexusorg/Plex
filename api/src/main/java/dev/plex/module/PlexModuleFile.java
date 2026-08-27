package dev.plex.module;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Information read from a module's module.yml file.
 * A module name can contain letters, digits, periods, underscores, and hyphens.
 * Its maximum length is 64 characters.
 */
public final class PlexModuleFile
{
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
    private final String name;
    private final String main;
    private final String description;
    private final String version;
    private final int apiCompatibility;
    private final List<String> libraries;
    private final List<String> repositories;
    private final boolean updaterEnabled;
    private final List<String> updateUrls;

    /**
     * Creates module information.
     *
     * @param name module name
     * @param main main module class
     * @param description module description
     * @param version module version
     * @param apiCompatibility required Plex API compatibility version
     */
    public PlexModuleFile(String name, String main, String description, String version, int apiCompatibility)
    {
        this(name, main, description, version, apiCompatibility, List.of(), List.of(), true, List.of());
    }

    /**
     * Creates module data.
     *
     * @param name module name
     * @param main main module class
     * @param description module description
     * @param version module version
     * @param apiCompatibility required Plex API version
     * @param libraries dependency libraries
     * @param repositories Maven repositories
     * @param updaterEnabled whether module updates are enabled
     * @param updateUrls custom update base URLs
     */
    public PlexModuleFile(String name, String main, String description, String version, int apiCompatibility,
                          List<String> libraries, List<String> repositories, boolean updaterEnabled,
                          List<String> updateUrls)
    {
        if (name == null || !NAME_PATTERN.matcher(name).matches())
        {
            throw new IllegalArgumentException("name is not a valid module name");
        }
        this.name = name;
        this.main = Objects.requireNonNull(main, "main");
        this.description = Objects.requireNonNull(description, "description");
        this.version = Objects.requireNonNull(version, "version");
        this.apiCompatibility = apiCompatibility;
        this.libraries = List.copyOf(Objects.requireNonNull(libraries, "libraries"));
        this.repositories = List.copyOf(Objects.requireNonNull(repositories, "repositories"));
        this.updaterEnabled = updaterEnabled;
        this.updateUrls = updateUrls == null ? List.of() : List.copyOf(updateUrls);
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
     * Returns Maven repositories declared by the module.
     *
     * @return Maven repositories declared by the module
     */
    public List<String> getRepositories()
    {
        return repositories;
    }

    /**
     * Returns whether Plex should include this module in module update commands.
     *
     * @return {@code true} when module updates are enabled
     */
    public boolean isUpdaterEnabled()
    {
        return updaterEnabled;
    }

    /**
     * Returns custom updater base URLs declared by the module.
     *
     * <p>An empty list tells Plex to use its default update URLs.</p>
     *
     * @return custom updater base URLs
     */
    public List<String> getUpdateUrls()
    {
        return updateUrls;
    }

}
