package dev.plex.admin;

import dev.plex.rank.enums.Rank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class Admin
{
    @Setter(AccessLevel.NONE)
    private UUID uuid;

    private Rank rank;

    private boolean commandSpy = true;
    private boolean staffChat = false;

    public Admin(UUID uuid)
    {
        this.uuid = uuid;
        this.rank = Rank.ADMIN;
    }


}
