package dev.plex.api.punishment;

import java.util.List;
import java.util.UUID;

public interface IndefiniteBanView
{
    List<String> usernames();
    List<UUID> uuids();
    List<String> ips();
    String reason();
}
