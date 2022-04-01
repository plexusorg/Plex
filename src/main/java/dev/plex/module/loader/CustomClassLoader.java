package dev.plex.module.loader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class CustomClassLoader extends URLClassLoader {
    /*public CustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
        for (URL url : urls) {
            super.addURL(url);
        }
    }*/

    public CustomClassLoader(URL jarInJar, ClassLoader parent) {
        super(new URL[]{extractJar(jarInJar)}, parent);
        addURL(jarInJar);
    }



    static URL extractJar(URL jarInJar) throws RuntimeException {
        // get the jar-in-jar resource
        if (jarInJar == null) {
            throw new RuntimeException("Could not locate jar-in-jar");
        }

        // create a temporary file
        // on posix systems by default this is only read/writable by the process owner
        Path path;
        try {
            path = Files.createTempFile("plex-jarinjar", ".jar.tmp");
        } catch (IOException e) {
            throw new RuntimeException("Unable to create a temporary file", e);
        }

        // mark that the file should be deleted on exit
        path.toFile().deleteOnExit();

        // copy the jar-in-jar to the temporary file path
        try (InputStream in = jarInJar.openStream()) {
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Unable to copy jar-in-jar to temporary path", e);
        }

        try {
            return path.toUri().toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to get URL from path", e);
        }
    }
}
