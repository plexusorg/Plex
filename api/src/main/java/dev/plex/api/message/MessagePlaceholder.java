package dev.plex.api.message;

import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A named value used to resolve a configured message placeholder.
 */
public final class MessagePlaceholder
{
    private final String name;
    private final String value;
    private final Component component;

    private MessagePlaceholder(String name, String value, @Nullable Component component)
    {
        if (!name.matches("[a-z][a-z0-9_]*"))
        {
            throw new IllegalArgumentException("Invalid message placeholder name: " + name);
        }
        this.name = name;
        this.value = value;
        this.component = component;
    }

    /** Creates a scalar placeholder whose value may contain MiniMessage markup. */
    public static MessagePlaceholder placeholder(String name, Object value)
    {
        return new MessagePlaceholder(name, String.valueOf(value), null);
    }

    /**
     * Creates a component placeholder. Component placeholders may be used only in message text,
     * not inside MiniMessage tag arguments.
     */
    public static MessagePlaceholder placeholder(String name, Component value)
    {
        return new MessagePlaceholder(name, null, Objects.requireNonNull(value));
    }

    /** Returns the placeholder name without braces. */
    public String name()
    {
        return name;
    }

    /** Returns the scalar value, or {@code null} for a component placeholder. */
    @Nullable
    public String value()
    {
        return value;
    }

    /** Returns the component value, or {@code null} for a scalar placeholder. */
    @Nullable
    public Component component()
    {
        return component;
    }
}
