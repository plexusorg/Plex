package dev.plex.command.annotation;

public @interface System
{
    String value() default "";

    boolean debug() default false;
}
