package dev.plex.module;

import com.google.common.collect.ImmutableList;
import lombok.Data;

import java.util.List;

@Data
public class PlexModuleFile
{
    private final String name;
    private final String main;
    private final String description;
    private final String version;

    //TODO: does not work
    private List<String> libraries = ImmutableList.of();
}
