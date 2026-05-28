package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.module.ModuleManager;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import dev.plex.util.BuildInfo;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.util.List;
import java.util.stream.Collectors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class PlexCMD extends ServerCommand
{
    public PlexCMD()
    {
        super(command("plex")
            .description("Show information about Plex or reload it")
            .usage("/<command> [reload | update | modules [reload | update | install <name> | uninstall <name> [-rmdir]]]")
            .build());
    }
    // Don't modify this command
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("reload")
                .executes(context -> executeCommand(context, "reload")));
        command.then(literal("update")
                .executes(context -> executeCommand(context, "update")));
        command.then(literal("modules")
                .executes(context -> executeCommand(context, "modules"))
                .then(literal("reload")
                        .executes(context -> executeCommand(context, "modules", "reload")))
                .then(literal("update")
                        .executes(context -> executeCommand(context, "modules", "update")))
                .then(literal("install")
                        .then(word("name")
                                .executes(context -> executeCommand(context, "modules", "install", string(context, "name")))))
                .then(literal("uninstall")
                        .then(word("name")
                                .suggests(suggest(() -> plugin.getModuleManager().getModules().stream()
                                        .map(module -> module.getPlexModuleFile().getName())
                                        .collect(Collectors.toList())))
                                .executes(context -> executeCommand(context, "modules", "uninstall", string(context, "name")))
                                .then(literal("-rmdir")
                                        .executes(context -> executeCommand(context, "modules", "uninstall", string(context, "name"), "-rmdir"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        String[] args = context.args();
        if (args.length == 0)
        {
            context.send(sender, context.mmString("<light_purple>Plex - A new freedom plugin."));
            context.send(sender, context.mmString("<light_purple>Plugin version: <gold>" + plugin.getPluginMeta().getVersion() + " #" + BuildInfo.getNumber() + " <light_purple>Git: <gold>" + BuildInfo.shortenCommit(BuildInfo.getCommit())));
            context.send(sender, context.mmString("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
            context.send(sender, context.mmString("<light_purple>Built by: <gold>" + BuildInfo.getAuthor() + " <light_purple>on <gold>" + BuildInfo.getDate()));
            context.send(sender, context.mmString("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
            plugin.getUpdateChecker().getUpdateStatusMessage(sender, true, 2);
            return null;
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            context.checkPermission(sender, "plex.reload");
            plugin.config.load();
            PlexLog.setDebugEnabled(plugin.config.getBoolean("debug"));
            context.send(sender, "Reloaded config file");
            plugin.messages.load();
            PlexUtils.configure(plugin.config, plugin.messages);
            context.send(sender, "Reloaded messages file");
            plugin.indefBans.load(false);
            plugin.getPunishmentManager().mergeIndefiniteBans();
            context.send(sender, "Reloaded indefinite bans");
            if (!plugin.getServer().getPluginManager().isPluginEnabled("Vault"))
            {
                throw new RuntimeException("Vault is required to run on the server if you use permissions!");
            }
            plugin.getServiceManager().endServices();
            plugin.getServiceManager().startServices();
            context.send(sender, "Restarted services.");
            TimeUtils.TIMEZONE = plugin.config.getString("server.timezone");
            context.send(sender, "Set timezone to: " + TimeUtils.TIMEZONE);
            context.send(sender, "Plex successfully reloaded.");
            return null;
        }
        else if (args[0].equalsIgnoreCase("modules"))
        {
            if (args.length == 1)
            {
                return context.mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
            }
            if (args[1].equalsIgnoreCase("reload"))
            {
                context.checkPermission(sender, "plex.modules.reload");
                plugin.getModuleManager().reloadModules();
                return context.mmString("<green>All modules reloaded!");
            }
            else if (args[1].equalsIgnoreCase("update"))
            {
                context.checkPermission(sender, "plex.modules.update");
                for (PlexModule module : plugin.getModuleManager().getModules())
                {
                    plugin.getUpdateChecker().updateModuleJar(sender, module);
                }
                plugin.getModuleManager().reloadModules();
                return context.mmString("<green>All modules updated and reloaded!");
            }
            else if (args[1].equalsIgnoreCase("install"))
            {
                context.checkPermission(sender, "plex.modules.install");
                if (args.length < 3)
                {
                    return context.usage();
                }
                String moduleName = args[2];
                plugin.getUpdateChecker().installModuleJar(sender, moduleName);
                return context.mmString("<green>Installing module <yellow>" + moduleName + "<green>...");
            }
            else if (args[1].equalsIgnoreCase("uninstall"))
            {
                context.checkPermission(sender, "plex.modules.uninstall");
                if (args.length < 3)
                {
                    return context.usage();
                }
                String moduleName = args[2];
                boolean removeData = args.length >= 4 && args[3].equalsIgnoreCase("-rmdir");
                ModuleManager.UninstallResult result = plugin.getModuleManager().uninstallModule(moduleName, removeData);
                switch (result)
                {
                    case NOT_FOUND:
                        return context.mmString("<red>No installed module named <yellow>" + moduleName + "<red> was found.");
                    case FAILED:
                        return context.mmString("<red>Failed to delete the JAR for <yellow>" + moduleName + "<red>. Check the server log.");
                    case REMOVED:
                        context.send(sender, context.mmString("<green>Uninstalled module <yellow>" + moduleName + "<green>" + (removeData ? " and its data folder" : "") + "."));
                        return context.messageComponent("moduleRestartRequired");
                }
            }
        }
        else if (args[0].equalsIgnoreCase("update"))
        {
            context.checkPermission(sender, "plex.update");
            if (!plugin.getUpdateChecker().getUpdateStatusMessage(sender, false, 0))
            {
                return context.mmString("<red>Plex is already up to date!");
            }
            plugin.getUpdateChecker().updateJar(sender, "Plex", false);
            return context.mmString("<red>Alert: Restart the server for the new JAR file to be applied.");
        }
        else
        {
            return context.usage();
        }
        return null;
    }
}
