package dev.plex.module;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.module.exception.ModuleLoadException;
import dev.plex.util.PlexLog;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Getter
public class ModuleManager
{
    private final List<PlexModule> modules = Lists.newArrayList();

    public ModuleManager()
    {
    }

    public void loadAllModules()
    {
        this.modules.clear();
        PlexLog.debug(String.valueOf(Plex.get().getModulesFolder().listFiles().length));
        Arrays.stream(Plex.get().getModulesFolder().listFiles()).forEach(file ->
        {
            if (file.getName().endsWith(".jar"))
            {
                try
                {
                    URLClassLoader loader = new URLClassLoader(
                            new URL[]{file.toURI().toURL()},
                            Plex.class.getClassLoader()
                    );

                    InputStreamReader internalModuleFile = new InputStreamReader(loader.getResourceAsStream("module.yml"), StandardCharsets.UTF_8);
                    YamlConfiguration internalModuleConfig = YamlConfiguration.loadConfiguration(internalModuleFile);

                    String name = internalModuleConfig.getString("name");
                    if (name == null)
                    {
                        throw new ModuleLoadException("Plex module name can't be null!");
                    }

                    String main = internalModuleConfig.getString("main");
                    if (main == null)
                    {
                        throw new ModuleLoadException("Plex module main class can't be null!");
                    }

                    String description = internalModuleConfig.getString("description", "A Plex module");
                    String version = internalModuleConfig.getString("version", "1.0");
                    List<String> libraries = internalModuleConfig.getStringList("libraries");

                    PlexModuleFile plexModuleFile = new PlexModuleFile(name, main, description, version);
                    plexModuleFile.setLibraries(libraries);
                    Class<? extends PlexModule> module = (Class<? extends PlexModule>) Class.forName(main, true, loader);

                    PlexModule plexModule = module.getConstructor().newInstance();
                    plexModule.setPlex(Plex.get());
                    plexModule.setPlexModuleFile(plexModuleFile);

                    plexModule.setDataFolder(new File(Plex.get().getModulesFolder() + File.separator + plexModuleFile.getName()));
                    if (!plexModule.getDataFolder().exists())
                    {
                        plexModule.getDataFolder().mkdir();
                    }

                    plexModule.setLogger(LogManager.getLogger(plexModuleFile.getName()));
                    modules.add(plexModule);
                }
                catch (MalformedURLException | ClassNotFoundException | InvocationTargetException |
                       InstantiationException | IllegalAccessException | NoSuchMethodException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    public void loadModules()
    {
        this.modules.forEach(module ->
        {
            PlexLog.log("Loading module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            module.load();
            //            this.libraryLoader.createLoader(module, module.getPlexModuleFile());
        });
    }

    public void enableModules()
    {
        this.modules.forEach(module ->
        {
            PlexLog.log("Enabling module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            module.enable();
        });
    }

    public void disableModules()
    {
        this.modules.forEach(module ->
        {
            PlexLog.log("Disabling module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            module.getCommands().stream().toList().forEach(plexCommand ->
            {
                module.unregisterCommand(plexCommand);
                Plex.get().getServer().getCommandMap().getKnownCommands().remove(plexCommand.getName());
                plexCommand.unregister(Plex.get().getServer().getCommandMap());
                try
                {
                    Plex.get().getServer().getCommandMap().getCommand(plexCommand.getName()).unregister(Plex.get().getServer().getCommandMap());
                }
                catch (Exception ignored)
                {

                }
                plexCommand.getAliases().forEach(alias -> Plex.get().getServer().getCommandMap().getKnownCommands().remove(alias));
            });
            module.getListeners().stream().toList().forEach(module::unregisterListener);
            module.disable();
        });
    }

    public void unloadModules()
    {
        this.disableModules();
        this.modules.forEach(module ->
        {
            try
            {
                ((URLClassLoader) module.getClass().getClassLoader()).close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
    }

    public void reloadModules()
    {
        unloadModules();
        loadAllModules();
        loadModules();
        enableModules();
    }
}
