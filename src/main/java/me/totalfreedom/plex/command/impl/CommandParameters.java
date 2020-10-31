package me.totalfreedom.plex.command.impl;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface CommandParameters
{
    String description() default "";
    String usage() default "/<command>";
    String aliases() default "";
}