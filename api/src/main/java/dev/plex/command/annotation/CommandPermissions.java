package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares permission and command-source requirements for a Plex command.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    /**
     * Returns the permission node required to use the command.
     *
     * @return permission node required to use the command
     */
    String permission() default "";

    /**
     * Returns the command source required to run the command.
     *
     * @return command source required to run the command
     */
    RequiredCommandSource source() default RequiredCommandSource.ANY;
}
