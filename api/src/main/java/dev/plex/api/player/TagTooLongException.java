package dev.plex.api.player;

/** Identifies a tag that exceeds the configured visible-text limit. */
public final class TagTooLongException extends IllegalArgumentException
{
    /** @serial The configured visible-text limit. */
    private final int maximumLength;

    /**
     * Creates a tag length failure.
     * @param maximumLength configured maximum number of characters
     */
    public TagTooLongException(int maximumLength)
    {
        super("Your tag cannot be longer than " + maximumLength + " characters.");
        this.maximumLength = maximumLength;
    }

    /**
     * Returns the limit used to validate the tag.
     * @return maximum number of characters
     */
    public int maximumLength()
    {
        return maximumLength;
    }
}
