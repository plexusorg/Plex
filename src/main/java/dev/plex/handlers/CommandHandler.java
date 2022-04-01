package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.PlexBase;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.System;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class CommandHandler extends PlexBase
{
    public CommandHandler()
    {
        Set<Class<? extends PlexCommand>> commandSet = PlexUtils.getClassesBySubType("dev.plex.command.impl", PlexCommand.class);
        List<PlexCommand> commands = Lists.newArrayList();

        commandSet.forEach(clazz ->
        {
            try
            {
                System annotation = clazz.getDeclaredAnnotation(System.class);
                // TODO: Annotations are always null?
                if (annotation != null)
                {
                    PlexLog.debug(clazz.getName() + " has annotations");
                    if (annotation.value().equalsIgnoreCase(plugin.getSystem().toLowerCase()))
                    {
                        commands.add(clazz.getConstructor().newInstance());
                        PlexLog.debug("Adding " + clazz.getName() + " as a rank command");
                    }

                    if (plugin.config.getBoolean("debug") && annotation.debug())
                    {
                        commands.add(clazz.getConstructor().newInstance());
                        PlexLog.debug("Adding " + clazz.getName() + " as a debug command");
                    }
                }
                else
                {
                    commands.add(clazz.getConstructor().newInstance());
                    // PlexLog.debug("Adding command normally " + clazz.getName());
                }
            }
            catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException ex)
            {
                PlexLog.error("Failed to register " + clazz.getSimpleName() + " as a command!");
            }
        });

        PlexLog.debug("Test");
        PlexLog.log(String.format("Registered %s commands from %s classes!", commands.size(), commandSet.size()));
    }
}
