package dev.plex.api.command;

import java.util.UUID;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;

/** Carries an attributed command name through a synchronous Plex command dispatch. */
@ApiStatus.Internal
public final class CommandExecutionIdentity
{
    private static final ThreadLocal<Identity> IDENTITY = new ThreadLocal<>();

    private CommandExecutionIdentity()
    {
    }

    public static <T> T call(UUID uniqueId, String name, Supplier<T> action)
    {
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

    public static String currentName(String fallback)
    {
        Identity identity = IDENTITY.get();
        return identity == null || identity.name() == null || identity.name().isBlank() ? fallback : identity.name();
    }

    public static UUID currentUniqueId()
    {
        Identity identity = IDENTITY.get();
        return identity == null ? null : identity.uniqueId();
    }

    private record Identity(UUID uniqueId, String name) { }
}
