package dev.plex.permission;

import dev.morphia.annotations.Entity;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
public class Permission
{
    private final UUID uuid;
    private final String permission;
    private boolean allowed = true;
}
