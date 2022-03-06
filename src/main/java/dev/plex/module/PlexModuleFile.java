package dev.plex.module;

import lombok.Data;

@Data
public class PlexModuleFile
{
    private final String name;
    private final String main;
    private final String description;
    private final String version;
}
