package dev.plex.command.annotation;

import com.mojang.brigadier.arguments.StringArgumentType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Taah
 * @since 4:31 AM [08-07-2023]
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Argument
{
    String value();

    StringArgumentType.StringType argumentType() default StringArgumentType.StringType.SINGLE_WORD;

    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
}
