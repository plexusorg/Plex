package dev.plex.module;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Data;

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
