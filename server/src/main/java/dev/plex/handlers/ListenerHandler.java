package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggleable;
import dev.plex.util.PlexLog;
import dev.plex.util.ReflectionsUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class ListenerHandler implements PlexBase
{
    public ListenerHandler()
    {
        Set<Class<? extends PlexListener>> listenerSet = ReflectionsUtil.getClassesBySubType("dev.plex.listener.impl", PlexListener.class);
        List<PlexListener> listeners = Lists.newArrayList();

        listenerSet.forEach(clazz ->
        {
            try
            {
                if (clazz.isAnnotationPresent(Toggleable.class))
                {
                    Toggleable annotation = clazz.getDeclaredAnnotation(Toggleable.class);
                    if (plugin.config.get(annotation.value()) != null && plugin.config.getBoolean(annotation.value()))
                    {
                        listeners.add(clazz.getConstructor().newInstance());
                    }
                }
                else
                {
                    listeners.add(clazz.getConstructor().newInstance());
                }
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                   NoSuchMethodException ex)
            {
                PlexLog.error("Failed to register " + clazz.getSimpleName() + " as a listener!");
            }
        });
        PlexLog.log(String.format("Registered %s listeners from %s classes!", listeners.size(), listenerSet.size()));
    }
}
