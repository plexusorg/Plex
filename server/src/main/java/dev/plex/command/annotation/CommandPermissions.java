package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Storage for the command's permissions
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    /**
     * Required command source
     *
     * @return The required command source of the command
     * @see RequiredCommandSource
     */
    RequiredCommandSource source() default RequiredCommandSource.ANY;

    /**
     * The permission
     *
     * @return Permission of the command
     */
    String permission() default ""; // No idea what to put here
}