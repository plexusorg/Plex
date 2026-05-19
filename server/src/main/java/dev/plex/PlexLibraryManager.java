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
    @Override
    public void classloader(@NotNull PluginClasspathBuilder classpathBuilder)
    {
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        PluginLibraries pluginLibraries = load();
        var repositoryUrls = new HashSet<String>();
        pluginLibraries.asRepositories().forEach(repository ->
        {
            repositoryUrls.add(repository.getUrl());
            resolver.addRepository(repository);
        });
        loadModuleRepositories()
                .filter(repository -> repositoryUrls.add(repository.getUrl()))
                .forEach(resolver::addRepository);
        pluginLibraries.asDependencies().forEach(resolver::addDependency);
        loadModuleDependencies().forEach(resolver::addDependency);
        classpathBuilder.addLibrary(resolver);
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
                .map(entry -> new RemoteRepository.Builder(entry.getKey(), "default", entry.getValue()).build());
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
            return repositories.entrySet().stream().map(e -> new RemoteRepository.Builder(e.getKey(), "default", e.getValue()).build());
        }
    }
}
