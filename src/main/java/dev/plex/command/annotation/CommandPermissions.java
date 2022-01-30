package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    Rank level() default Rank.IMPOSTOR;

    RequiredCommandSource source() default RequiredCommandSource.ANY;

    String permission() default ""; // No idea what to put here
}