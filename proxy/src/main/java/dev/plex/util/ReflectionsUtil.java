package dev.plex.util;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import dev.plex.Plex;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ReflectionsUtil
{
    @SuppressWarnings("UnstableApiUsage")
    public static Set<Class<?>> getClassesFrom(String packageName)
    {
        Set<Class<?>> classes = new HashSet<>();
        try
        {
            ClassPath path = ClassPath.from(Plex.class.getClassLoader());
            ImmutableSet<ClassPath.ClassInfo> infoSet = path.getTopLevelClasses(packageName);
            infoSet.forEach(info ->
            {
                try
                {
                    Class<?> clazz = Class.forName(info.getName());
                    classes.add(clazz);
                }
                catch (ClassNotFoundException ex)
                {
                    PlexLog.error("Unable to find class " + info.getName() + " in " + packageName);
                }
            });
        }
        catch (IOException ex)
        {
            PlexLog.error("Something went wrong while fetching classes from " + packageName);
            throw new RuntimeException(ex);
        }
        return Collections.unmodifiableSet(classes);
    }

    @SuppressWarnings("unchecked")
    public static <T> Set<Class<? extends T>> getClassesBySubType(String packageName, Class<T> subType)
    {
        Set<Class<?>> loadedClasses = getClassesFrom(packageName);
        Set<Class<? extends T>> classes = new HashSet<>();
        loadedClasses.forEach(clazz ->
        {
            if (clazz.getSuperclass() == subType || Arrays.asList(clazz.getInterfaces()).contains(subType))
            {
                classes.add((Class<? extends T>) clazz);
            }
        });
        return Collections.unmodifiableSet(classes);
    }
}
