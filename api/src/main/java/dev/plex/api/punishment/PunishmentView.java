package dev.plex.api.punishment;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Read-only view of a Plex punishment.
 */
public interface PunishmentView
{
    /**
     * Returns the UUID of the punished player.
     *
     * @return UUID of the punished player
     */
    UUID punished();

    /**
     * Returns the UUID of the actor who issued the punishment.
     *
     * @return UUID of the actor who issued the punishment
     */
    UUID punisher();

    /**
     * Returns the display name of the actor who issued the punishment.
     *
     * @return display name of the actor who issued the punishment
     */
    String punisherName();

    /**
     * Returns the IP address associated with the punished player.
     *
     * @return IP address associated with the punished player
     */
    String ip();

    /**
     * Returns the username of the punished player.
     *
     * @return username of the punished player
     */
    String punishedUsername();

    /**
     * Returns the punishment type.
     *
     * @return punishment type
     */
    PunishmentType type();

    /**
     * Returns the punishment reason.
     *
     * @return punishment reason
     */
    String reason();

    /**
     * Returns whether the punishment uses a custom duration.
     *
     * @return whether the punishment uses a custom duration
     */
    boolean customTime();

    /**
     * Returns whether the punishment is currently active.
     *
     * @return whether the punishment is currently active
     */
    boolean active();

    /**
     * Returns the date and time when the punishment was issued.
     *
     * @return date and time when the punishment was issued
     */
    ZonedDateTime issueDate();

    /**
     * Returns the punishment end date.
     *
     * @return punishment end date, or {@code null} for punishments without an end date
     */
    ZonedDateTime endDate();
}
