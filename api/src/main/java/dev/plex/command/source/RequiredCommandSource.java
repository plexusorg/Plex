package dev.plex.command.source;

/**
 * Source restrictions for command execution.
 */
public enum RequiredCommandSource
{
    /**
     * Allows both player and console sources.
     */
    ANY,

    /**
     * Allows only in-game player sources.
     */
    IN_GAME,

    /**
     * Allows only console sources.
     */
    CONSOLE
}
