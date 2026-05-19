package dev.plex;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public class PlexLibraryManager implements PluginLoader
{
    private static final List<String> MAVEN_CENTRAL_URLS = List.of(
            "https://repo1.maven.org/maven2",
            "http://repo1.maven.org/maven2",
            "https://repo.maven.apache.org/maven2",
            "http://repo.maven.apache.org/maven2"
    );

    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder)
    {
        PluginLibraries pluginLibraries = load();
        List<RemoteRepository> pluginRepositories = pluginLibraries.asRepositories().toList();
        List<RemoteRepository> moduleRepositories = loadModuleRepositories().toList();
        List<Dependency> moduleDependencies = loadModuleDependencies().toList();

        if (!moduleDependencies.isEmpty())
        {
            MavenLibraryResolver moduleResolver = new MavenLibraryResolver();
            addRepositories(moduleResolver, Stream.concat(moduleRepositories.stream(), pluginRepositories.stream()));
            moduleDependencies.forEach(moduleResolver::addDependency);
            classpathBuilder.addLibrary(moduleResolver);
        }

        MavenLibraryResolver pluginResolver = new MavenLibraryResolver();
        addRepositories(pluginResolver, pluginRepositories.stream());
        pluginLibraries.asDependencies().forEach(pluginResolver::addDependency);
        classpathBuilder.addLibrary(pluginResolver);
    }

    public PluginLibraries load()
    {
        try (var in = getClass().getResourceAsStream("/paper-libraries.json"))
        {
            return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), PluginLibraries.class);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private Stream<Dependency> loadModuleDependencies()
    {
        File modulesFolder = new File("plugins/Plex/modules");
        File[] moduleFiles = modulesFolder.listFiles((directory, name) -> name.endsWith(".jar"));

        if (moduleFiles == null)
        {
            return Stream.empty();
        }

        return Arrays.stream(moduleFiles)
                .flatMap(this::readModuleLibraries)
                .filter(library -> !library.isBlank())
                .distinct()
                .map(library -> new Dependency(new DefaultArtifact(library), null));
    }

    private Stream<RemoteRepository> loadModuleRepositories()
    {
        File modulesFolder = new File("plugins/Plex/modules");
        File[] moduleFiles = modulesFolder.listFiles((directory, name) -> name.endsWith(".jar"));

        if (moduleFiles == null)
        {
            return Stream.empty();
        }

        return Arrays.stream(moduleFiles)
                .flatMap(this::readModuleRepositories)
                .distinct();
    }

    private Stream<String> readModuleLibraries(File moduleFile)
    {
        YamlConfiguration moduleYml = readModuleYml(moduleFile);
        if (moduleYml == null)
        {
            return Stream.empty();
        }
        return moduleYml.getStringList("libraries").stream();
    }

    private Stream<RemoteRepository> readModuleRepositories(File moduleFile)
    {
        YamlConfiguration moduleYml = readModuleYml(moduleFile);
        if (moduleYml == null)
        {
            return Stream.empty();
        }

        var repositories = moduleYml.getConfigurationSection("repositories");
        if (repositories == null)
        {
            return Stream.empty();
        }

        return repositories.getKeys(false).stream()
                .map(id -> Map.entry(id, repositories.getString(id, "")))
                .filter(entry -> !entry.getValue().isBlank())
                .flatMap(entry -> runtimeRepository(entry.getKey(), entry.getValue()));
    }

    private YamlConfiguration readModuleYml(File moduleFile)
    {
        try (JarFile jarFile = new JarFile(moduleFile))
        {
            var moduleYml = jarFile.getJarEntry("module.yml");

            if (moduleYml == null)
            {
                return null;
            }

            try (var in = new InputStreamReader(jarFile.getInputStream(moduleYml), StandardCharsets.UTF_8))
            {
                return YamlConfiguration.loadConfiguration(in);
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to read module metadata from " + moduleFile.getName(), e);
        }
    }

    private record PluginLibraries(Map<String, String> repositories, List<String> dependencies)
    {
        public Stream<Dependency> asDependencies()
        {
            return dependencies.stream().map(d -> new Dependency(new DefaultArtifact(d), null));
        }

        public Stream<RemoteRepository> asRepositories()
        {
            return repositories.entrySet().stream().flatMap(e -> runtimeRepository(e.getKey(), e.getValue()));
        }
    }

    private static Stream<RemoteRepository> runtimeRepository(String id, String url)
    {
        String runtimeUrl = MAVEN_CENTRAL_URLS.stream().anyMatch(url::startsWith)
                ? MavenLibraryResolver.MAVEN_CENTRAL_DEFAULT_MIRROR
                : url;

        if (!runtimeUrl.startsWith("https://") && !runtimeUrl.startsWith("http://"))
        {
            return Stream.empty();
        }

        return Stream.of(new RemoteRepository.Builder(id, "default", runtimeUrl).build());
    }

    private static void addRepositories(MavenLibraryResolver resolver, Stream<RemoteRepository> repositories)
    {
        var repositoryUrls = new HashSet<String>();
        repositories
                .filter(repository -> repositoryUrls.add(repository.getUrl()))
                .forEach(resolver::addRepository);
    }
}
