package dev.plex.punishment;

public enum PunishmentType
{
    MUTE, FREEZE, BAN, KICK, SMITE, TEMPBAN;

    public static final java.time.Duration STANDARD_BAN_DURATION =
            dev.plex.api.punishment.PunishmentType.STANDARD_BAN_DURATION;
}
