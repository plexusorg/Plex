package dev.plex.api.command;

import java.util.UUID;
import java.util.function.Supplier;
import java.util.Objects;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/** Carries an attributed command name through a synchronous Plex command dispatch. */
@ApiStatus.Internal
public final class CommandExecutionIdentity
{
    private static final ThreadLocal<Identity> IDENTITY = new ThreadLocal<>();

    private CommandExecutionIdentity()
    {
    }

    /**
     * Runs an action with a temporary command identity.
     *
     * @param uniqueId actor UUID, or {@code null}
     * @param name actor name, or {@code null}
     * @param action action to run
     * @param <T> result type
     * @return action result
     */
    public static <T> T call(@Nullable UUID uniqueId, @Nullable String name, Supplier<T> action)
    {
        Objects.requireNonNull(action, "action");
        Identity previous = IDENTITY.get();
        IDENTITY.set(new Identity(uniqueId, name));
        try
        {
            return action.get();
        }
        finally
        {
            if (previous == null)
            {
                IDENTITY.remove();
            }
            else
            {
                IDENTITY.set(previous);
            }
        }
    }

    /**
     * Returns the current actor name.
     *
     * @param fallback value to use when no name is set
     * @return current actor name or the fallback value
     */
    public static String currentName(String fallback)
    {
        Identity identity = IDENTITY.get();
        return identity == null || identity.name() == null || identity.name().isBlank() ? fallback : identity.name();
    }

    /**
     * Returns the current actor UUID.
     *
     * @return current actor UUID, or {@code null} when no UUID is set
     */
    @Nullable
    public static UUID currentUniqueId()
    {
        Identity identity = IDENTITY.get();
        return identity == null ? null : identity.uniqueId();
    }

    private record Identity(UUID uniqueId, String name) { }
}
