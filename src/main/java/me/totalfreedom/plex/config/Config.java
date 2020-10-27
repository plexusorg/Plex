package me.totalfreedom.plex.config;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.util.PlexLog;

import java.io.File;
import java.io.IOException;

public class Config
{

    private File file;


    public Config(String name, boolean copy)
    {
        if (copy)
        {
            Plex.get().saveResource(name, false);
        } else {
            file = new File(Plex.get().getDataFolder(), name);
            if (!file.exists())
            {
                try {
                    file.createNewFile();
                    PlexLog.log("Generating " + name + " configuration file!");
                } catch (IOException e) {
                    PlexLog.error(String.format("An error occured trying to create the following file: %s", name));
                    e.printStackTrace();
                }
            } else {
                PlexLog.log(name + " configuration file was loaded.");
            }
        }
    }

    public File getFile() {
        return file;
    }
}
