package dev.plex.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Declares display and invocation metadata for a Plex command.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters
{
    /**
     * Returns the primary command name.
     *
     * @return primary command name
     */
    String name();

    /**
     * Returns the short command description.
     *
     * @return short command description
     */
    String description() default "";

    /**
     * Returns the command usage text.
     *
     * @return command usage text; {@code <command>} is replaced with the command name
     */
    String usage() default "/<command>";

    /**
     * Returns comma-separated command aliases.
     *
     * @return comma-separated command aliases
     */
    String aliases() default "";
}
