package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Taah
 * @project Plex
 * @since 7:08 AM [09-07-2023]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CommandSource
{
    RequiredCommandSource value() default RequiredCommandSource.ANY;
}
