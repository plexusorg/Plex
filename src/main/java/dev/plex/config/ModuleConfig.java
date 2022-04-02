package dev.plex.config;

import dev.plex.module.PlexModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Creates a custom Config object
 */
public class ModuleConfig extends YamlConfiguration
{
    /**
     * The plugin instance
     */
    private PlexModule module;

    /**
     * The File instance
     */
    private File file;

    /**
     * The file name
     */
    private String name;

    /**
     * Creates a config object
     *
     * @param module The module instance
     * @param name   The file name
     */
    public ModuleConfig(PlexModule module, String name)
    {
        this.module = module;
        this.file = new File(module.getDataFolder(), name);
        this.name = name;

        if (!file.exists())
        {
            saveDefault();
        }
    }

    public void load() throws IOException, InvalidConfigurationException
    {
        super.load(file);
    }

    /**
     * Saves the configuration file
     */
    public void save()
    {
        try
        {
            super.save(file);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * Moves the configuration file from the plugin's resources folder to the data folder (plugins/Plex/)
     */
    private void saveDefault()
    {
        try
        {
            Files.copy(module.getClass().getResourceAsStream("/" + name), this.file.toPath());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        /*if (name == null || name.equals("")) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        name = name.replace('\\', '/');
        InputStream in = module.getResource("/" + name);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + name + "'");
        }

        File outFile = new File(module.getDataFolder(), name);
        int lastIndex = name.lastIndexOf('/');
        File outDir = new File(module.getDataFolder(), name.substring(0, lastIndex >= 0 ? lastIndex : 0));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists()) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                module.getLogger().log(org.apache.logging.log4j.Level.INFO, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            module.getLogger().log(Level.ERROR, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }*/
    }
}