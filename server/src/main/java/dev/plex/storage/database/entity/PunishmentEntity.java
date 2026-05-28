package dev.plex.storage.database.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PunishmentEntity
{
    private long id;
    private String punishedUuid;
    private String punisherUuid;
    private String source;
    private String punisherReference;
    private String ip;
    private String type;
    private String reason;
    private boolean customTime;
    private boolean active;
    private long issueDate;
    private long endDate;

    public PunishmentEntity()
    {
    }
}
