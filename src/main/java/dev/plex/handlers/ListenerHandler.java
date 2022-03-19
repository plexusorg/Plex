package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggled;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class ListenerHandler extends PlexBase
{
    public ListenerHandler()
    {
        Set<Class<? extends PlexListener>> listenerSet = PlexUtils.getClassesBySubType("dev.plex.listener.impl", PlexListener.class);
        List<PlexListener> listeners = Lists.newArrayList();

        listenerSet.forEach(clazz ->
        {
            try
            {
                Toggled annotation = clazz.getDeclaredAnnotation(Toggled.class);
                if (annotation != null)
                {
                    if (plugin.config.getBoolean("chat.enabled") && annotation.enabled())
                    {
                        listeners.add(clazz.getConstructor().newInstance());
                    }
                }
                else
                {
                    listeners.add(clazz.getConstructor().newInstance());
                }
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex)
            {
                PlexLog.error("Failed to register " + clazz.getSimpleName() + " as a listener!");
            }
        });
        PlexLog.log(String.format("Registered %s listeners from %s classes!", listeners.size(), listenerSet.size()));
    }
}
