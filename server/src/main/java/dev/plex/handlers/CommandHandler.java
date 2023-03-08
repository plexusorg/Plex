package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.System;
import dev.plex.util.PlexLog;
import dev.plex.util.ReflectionsUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class CommandHandler implements PlexBase
{
    public CommandHandler()
    {
        Set<Class<? extends PlexCommand>> commandSet = ReflectionsUtil.getClassesBySubType("dev.plex.command.impl", PlexCommand.class);
        List<PlexCommand> commands = Lists.newArrayList();

        commandSet.forEach(clazz ->
        {
            try
            {
                if (clazz.isAnnotationPresent(System.class))
                {
                    System annotation = clazz.getDeclaredAnnotation(System.class);
                    if (annotation.value().equalsIgnoreCase(plugin.getSystem().toLowerCase()))
                    {
                        commands.add(clazz.getConstructor().newInstance());
                    }

                    if (plugin.config.getBoolean("debug") && annotation.debug())
                    {
                        commands.add(clazz.getConstructor().newInstance());
                    }
                }
                else
                {
                    commands.add(clazz.getConstructor().newInstance());
                }
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                   NoSuchMethodException ex)
            {
                PlexLog.error("Failed to register " + clazz.getSimpleName() + " as a command!");
            }
        });
        PlexLog.log(String.format("Registered %s commands from %s classes!", commands.size(), commandSet.size()));
    }
}
