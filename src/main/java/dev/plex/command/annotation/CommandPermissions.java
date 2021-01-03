package dev.plex.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    Rank level() default Rank.IMPOSTOR;

    RequiredCommandSource source() default RequiredCommandSource.ANY;
}