package dev.plex.config;

import dev.plex.module.PlexModule;
import dev.plex.util.PlexLog;
import org.apache.logging.log4j.Level;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Creates a custom Config object
 */
public class ModuleConfig extends YamlConfiguration {
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
     * Whether new entries were added to the file automatically
     */
    private boolean added = false;

    /**
     * Creates a config object
     *
     * @param module The module instance
     * @param name   The file name
     */
    public ModuleConfig(PlexModule module, String name) {
        this.module = module;
        this.file = new File(module.getDataFolder(), name);
        this.name = name;

        if (!file.exists()) {
            saveDefault();
        }
    }

    public void load() {
        this.load(true);
    }

    /**
     * Loads the configuration file
     */
    public void load(boolean loadFromFile) {
        try {
            if (loadFromFile) {
                YamlConfiguration externalYamlConfig = YamlConfiguration.loadConfiguration(file);
                InputStreamReader internalConfigFileStream = new InputStreamReader(module.getResource(name), StandardCharsets.UTF_8);
                YamlConfiguration internalYamlConfig = YamlConfiguration.loadConfiguration(internalConfigFileStream);

                // Gets all the keys inside the internal file and iterates through all of it's key pairs
                for (String string : internalYamlConfig.getKeys(true)) {
                    // Checks if the external file contains the key already.
                    if (!externalYamlConfig.contains(string)) {
                        // If it doesn't contain the key, we set the key based off what was found inside the plugin jar
                        externalYamlConfig.setComments(string, internalYamlConfig.getComments(string));
                        externalYamlConfig.set(string, internalYamlConfig.get(string));
                        PlexLog.log("Setting key: " + string + " in " + this.name + " to the default value(s) since it does not exist!");
                        added = true;
                    }
                }
                if (added) {
                    externalYamlConfig.save(file);
                    PlexLog.log("Saving new file...");
                    added = false;
                }
            }
            super.load(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Saves the configuration file
     */
    public void save() {
        try {
            super.save(file);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Moves the configuration file from the plugin's resources folder to the data folder (plugins/Plex/)
     */
    private void saveDefault() {
        try {
            Files.copy(module.getClass().getResourceAsStream("/" + name), this.file.toPath());
        } catch (IOException e) {
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