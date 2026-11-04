package dev.plex;

import com.google.gson.Gson;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.bukkit.configuration.InvalidConfigurationException;
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
        List<YamlConfiguration> modules = readModuleYmls(classpathBuilder.getContext().getLogger());
        List<RemoteRepository> moduleRepositories = modules.stream().flatMap(this::moduleRepositories).distinct().toList();
        List<Dependency> moduleDependencies = modules.stream()
                .flatMap(module -> module.getStringList("libraries").stream())
                .filter(library -> !library.isBlank())
                .distinct()
                .map(library -> new Dependency(new DefaultArtifact(library), null))
                .toList();

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

    // The module.yml of every module jar that has a valid one. This runs before the server exists, so it must not use
    // YamlConfiguration.loadConfiguration, which logs parse errors through Bukkit. A module with an invalid module.yml
    // gets no libraries here; ModuleManager rejects it with the parse error when it loads modules.
    private List<YamlConfiguration> readModuleYmls(ComponentLogger logger)
    {
        File[] moduleFiles = new File("plugins/Plex/modules").listFiles((directory, name) -> name.endsWith(".jar"));
        if (moduleFiles == null)
        {
            return List.of();
        }
        List<YamlConfiguration> modules = new ArrayList<>();
        for (File moduleFile : moduleFiles)
        {
            try (JarFile jarFile = new JarFile(moduleFile))
            {
                var moduleYml = jarFile.getJarEntry("module.yml");
                if (moduleYml == null)
                {
                    continue;
                }
                YamlConfiguration module = new YamlConfiguration();
                try (var in = new InputStreamReader(jarFile.getInputStream(moduleYml), StandardCharsets.UTF_8))
                {
                    module.load(in);
                }
                modules.add(module);
            }
            catch (InvalidConfigurationException e)
            {
                logger.warn("Module {} has an invalid module.yml, so its libraries are not loaded: {}", moduleFile.getName(),
                        e.getMessage());
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to read module metadata from " + moduleFile.getName(), e);
            }
        }
        return modules;
    }

    private Stream<RemoteRepository> moduleRepositories(YamlConfiguration module)
    {
        var repositories = module.getConfigurationSection("repositories");
        if (repositories == null)
        {
            return Stream.empty();
        }

        return repositories.getKeys(false).stream()
                .map(id -> Map.entry(id, repositories.getString(id, "")))
                .filter(entry -> !entry.getValue().isBlank())
                .flatMap(entry -> runtimeRepository(entry.getKey(), entry.getValue()));
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
