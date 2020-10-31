package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.rank.enums.Rank;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandPermissions
{
    Rank level() default Rank.IMPOSTOR;
    RequiredCommandSource source();
}