package dev.plex.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Storage for a command's parameters
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters
{
    /**
     * The name
     *
     * @return Name of the command
     */
    String name();

    /**
     * The description
     *
     * @return Description of the command
     */
    String description() default "";

    /**
     * The usage (optional)
     *
     * @return The usage of the command
     */
    String usage() default "/<command>";

    /**
     * The aliases (optional)
     *
     * @return The aliases of the command
     */
    String aliases() default "";
}