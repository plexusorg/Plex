package dev.plex.module;

import dev.plex.Plex;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Getter
@Setter(AccessLevel.MODULE)
public abstract class PlexModule
{
    private Plex plex;
    private PlexModuleFile plexModuleFile;
    private File dataFolder;
    private Logger logger;

    public void load() {}

    public void enable() {}

    public void disable() {}
}
