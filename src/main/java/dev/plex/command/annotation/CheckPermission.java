package dev.plex.command.annotation;

import dev.plex.rank.enums.Rank;

public @interface CheckPermission
{
    String permission() default "";

    Rank rank() default Rank.IMPOSTOR;

}
