package me.totalfreedom.plex.command.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    Rank level() default Rank.IMPOSTOR;

    RequiredCommandSource source() default RequiredCommandSource.ANY;
}