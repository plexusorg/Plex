package dev.plex.permission;

import dev.morphia.annotations.Entity;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
public class Permission
{
    private final UUID uuid;
    private final String permission;
    private boolean allowed = true;
}
