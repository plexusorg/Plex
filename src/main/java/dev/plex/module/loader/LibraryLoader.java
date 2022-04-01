package dev.plex.module.loader;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

//TODO: doesn't work

public class LibraryLoader {

    private final Logger logger;
    private final RepositorySystem repository;
    private final DefaultRepositorySystemSession session;
    private final List<RemoteRepository> repositories;

    public LibraryLoader(@NotNull Logger logger) {
        this.logger = logger;

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);

        this.repository = locator.getService(RepositorySystem.class);
        this.session = MavenRepositorySystemUtils.newSession();

        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, new LocalRepository("libraries")));
        session.setTransferListener(new AbstractTransferListener() {
            @Override
            public void transferStarted(@NotNull TransferEvent event) throws TransferCancelledException {
                logger.log(Level.INFO, "Downloading {0}", event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
            }
        });
        session.setReadOnly();

        this.repositories = repository.newResolutionRepositories(session, Arrays.asList(new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build()));
    }

    @Nullable
    public ClassLoader createLoader(@NotNull PlexModule module, @NotNull PlexModuleFile moduleFile) {
        if (moduleFile.getLibraries().isEmpty()) {
            return null;
        }
        logger.log(Level.INFO, "Loading libraries for {0}", new Object[]{moduleFile.getName()});
        logger.log(Level.INFO, "[{0}] Loading {1} libraries... please wait", new Object[]
                {
                        moduleFile.getName(), moduleFile.getLibraries().size()
                });

        List<Dependency> dependencies = new ArrayList<>();
        List<Class<?>> classes = Lists.newArrayList();
        List<File> files = Lists.newArrayList();
        for (String library : moduleFile.getLibraries()) {
            Artifact artifact = new DefaultArtifact(library);
            Dependency dependency = new Dependency(artifact, null);

            dependencies.add(dependency);
        }

        DependencyResult result;
        try {
            result = repository.resolveDependencies(session, new DependencyRequest(new CollectRequest((Dependency) null, dependencies, repositories), null));
        } catch (DependencyResolutionException ex) {
            throw new RuntimeException("Error resolving libraries", ex);
        }

        List<URL> jarFiles = new ArrayList<>();
        for (ArtifactResult artifact : result.getArtifactResults()) {
            File file = artifact.getArtifact().getFile();
            files.add(file);
            URL url;
            try {
                url = file.toURI().toURL();
            } catch (MalformedURLException ex) {
                throw new AssertionError(ex);
            }

            jarFiles.add(url);
            logger.log(Level.INFO, "[{0}] Loaded library {1}", new Object[]
                    {
                            moduleFile.getName(), file
                    });
        }

        /*List<URL> jarFiles = Lists.newArrayList();
        List<Artifact> artifacts = Lists.newArrayList();


        List<Class<?>> classes = new ArrayList<>();

        for (String library : moduleFile.getLibraries()) {
            Artifact artifact = new DefaultArtifact(library);
            ArtifactRequest request = new ArtifactRequest();
            request.setArtifact(artifact);
            request.addRepository(this.repositories.get(0));
            try {
                ArtifactResult result = this.repository.resolveArtifact(this.session, request);
                artifact = result.getArtifact();
                jarFiles.add(artifact.getFile().toURI().toURL());
                logger.log(Level.INFO, "Loaded library {0} for {1}", new Object[]{
                        artifact.getFile().toURI().toURL().toString(),
                        moduleFile.getName()
                });
                artifacts.add(artifact);
            } catch (ArtifactResolutionException | MalformedURLException e) {
                e.printStackTrace();
            }

        }*/
        logger.log(Level.INFO, "Loaded {0} libraries for {1}", new Object[]{jarFiles.size(), moduleFile.getName()});

//        jarFiles.forEach(jar -> new CustomClassLoader(jar, Plex.class.getClassLoader()));
//        jarFiles.forEach(jar -> new CustomClassLoader(jar, Plex.class.getClassLoader()));

        /*URLClassLoader loader = new URLClassLoader(jarFiles.toArray(URL[]::new), Plex.class.getClassLoader());

        dependencies.forEach(artifact -> {
            ArrayList<String> classNames;
            try {
                classNames = getClassNamesFromJar(new JarFile(artifact.getArtifact().getFile()));
                for (String className : classNames) {
                    Class<?> classToLoad = Class.forName(className, true, loader);
                    classes.add(classToLoad);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        classes.forEach(clazz -> logger.log(Level.INFO, "Loading class {0}", new Object[]{clazz.getName()}));*/
        jarFiles.forEach(url -> {
            JarURLConnection connection;
            try {
                URL url2 = new URL("jar:" + url.toString() + "!/");
                /*
                connection = (JarURLConnection) url2.openConnection();
                logger.log(Level.INFO, "Jar File: " + connection.getJarFileURL().toString());*/
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return new URLClassLoader(files.stream().map(File::toURI).map(uri -> {
            try {
                return uri.toURL();
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }).toList().toArray(URL[]::new)/*jarFiles.stream().map(url -> {
            try {
                return new URL("jar:" + url.toString() + "!/");
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
        }).toList().toArray(URL[]::new)*/, Plex.class.getClassLoader())/*new CustomClassLoader(jarFiles.toArray(URL[]::new), Plex.class.getClassLoader())*/;
    }

    /*public List<Class<?>> loadDependency(List<Path> paths) throws Exception {

        List<Class<?>> classes = new ArrayList<>();

        for (Path path : paths) {

            URL url = path.toUri().toURL();
            URLClassLoader child = new URLClassLoader(new URL[]{url}, this.getClass().getClassLoader());

            ArrayList<String> classNames = getClassNamesFromJar(path.toString());

            for (String className : classNames) {
                Class classToLoad = Class.forName(className, true, child);
                classes.add(classToLoad);
            }
        }

        return classes;
    }*/


    private ArrayList<String> getClassNamesFromJar(JarFile file) throws Exception {
        ArrayList<String> classNames = new ArrayList<>();
        try {
            //Iterate through the contents of the jar file
            Enumeration<JarEntry> entries = file.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                //Pick file that has the extension of .class
                if ((entry.getName().endsWith(".class"))) {
                    String className = entry.getName().replaceAll("/", "\\.");
                    String myClass = className.substring(0, className.lastIndexOf('.'));
                    classNames.add(myClass);
                }
            }
        } catch (Exception e) {
            throw new Exception("Error while getting class names from jar", e);
        }
        return classNames;
    }
}
