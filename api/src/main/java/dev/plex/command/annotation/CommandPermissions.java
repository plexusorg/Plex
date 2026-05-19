package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    String permission() default "";
    RequiredCommandSource source() default RequiredCommandSource.ANY;
}
