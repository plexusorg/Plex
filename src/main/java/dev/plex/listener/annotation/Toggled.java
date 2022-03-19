package dev.plex.listener.annotation;

public @interface Toggled
{
    boolean enabled() default false;
}
