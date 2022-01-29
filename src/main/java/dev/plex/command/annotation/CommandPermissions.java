package dev.plex.command.annotation;

import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.bukkit.permissions.Permission;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    Rank level() default Rank.IMPOSTOR;

    RequiredCommandSource source() default RequiredCommandSource.ANY;

    String permission() default "plex.donotgivethispermission"; // No idea what to put here
}