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
import dev.plex.util.UpdateChecker;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
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
        String[] args = context.args();
        if (args.length == 0)
        {
            return showInfo(context);
        }
        return switch (args[0].toLowerCase(Locale.ROOT))
        {
            case "reload" -> reload(context);
            case "modules" -> modules(context, args);
            case "update" -> update(context);
            default -> context.usage();
        };
    }

    private Component showInfo(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.send(sender, context.mmString("<light_purple>Plex - A new freedom plugin."));
        context.send(sender, context.mmString("<light_purple>Plugin version: <gold>" + plugin.getPluginMeta().getVersion() + " #" + BuildInfo.getNumber() + " <light_purple>Git: <gold>" + BuildInfo.shortenCommit(BuildInfo.getCommit())));
        context.send(sender, context.mmString("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
        context.send(sender, context.mmString("<light_purple>Built by: <gold>" + BuildInfo.getAuthor() + " <light_purple>on <gold>" + BuildInfo.getDate()));
        context.send(sender, context.mmString("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
        plugin.getApi().scheduler().runAsync(() -> plugin.getUpdateChecker().getUpdateStatusMessage(sender, true, 2));
        return null;
    }

    private Component reload(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.reload");
        plugin.config.load();
        PlexLog.setDebugEnabled(plugin.config.getBoolean("debug"));
        context.send(sender, "Reloaded config file");
        plugin.entities.load();
        context.send(sender, "Reloaded entities file");
        plugin.worlds.load();
        context.send(sender, "Reloaded worlds file");
        plugin.messages.load();
        PlexUtils.configure(plugin.config, plugin.messages);
        context.send(sender, "Reloaded messages file");
        plugin.toggles.load();
        context.send(sender, "Reloaded toggles file");
        plugin.indefBans.load(false);
        plugin.getPunishmentManager().mergeIndefiniteBans();
        context.send(sender, "Reloaded indefinite bans");
        plugin.getServiceManager().endServices();
        plugin.getServiceManager().startServices();
        context.send(sender, "Restarted services.");
        TimeUtils.TIMEZONE = plugin.config.getString("server.timezone", "Etc/UTC");
        context.send(sender, "Set timezone to: " + TimeUtils.TIMEZONE);
        context.send(sender, "Plex successfully reloaded.");
        return null;
    }

    private Component modules(ServerCommandContext context, String[] args)
    {
        if (args.length == 1)
        {
            return context.mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
        }
        return switch (args[1].toLowerCase(Locale.ROOT))
        {
            case "reload" -> reloadModules(context);
            case "update" -> updateModules(context);
            case "install" -> installModule(context, args);
            case "uninstall" -> uninstallModule(context, args);
            default -> context.usage();
        };
    }

    private Component reloadModules(ServerCommandContext context)
    {
        context.checkPermission(context.sender(), "plex.modules.reload");
        plugin.getModuleManager().reloadModules();
        return context.mmString("<green>All modules reloaded!");
    }

    private Component updateModules(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.modules.update");
        plugin.getApi().scheduler().runAsync(() ->
        {
            int updated = 0;
            int skipped = 0;
            int failed = 0;
            for (PlexModule module : plugin.getModuleManager().getModules())
            {
                switch (plugin.getUpdateChecker().updateModuleJar(sender, module))
                {
                    case UPDATED -> updated++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failed++;
                }
            }
            int updatedCount = updated;
            int skippedCount = skipped;
            int failedCount = failed;
            plugin.getApi().scheduler().runGlobal(() ->
            {
                plugin.getModuleManager().reloadModules();
                sender.sendMessage(context.messageComponent("moduleUpdateSummary", updatedCount, skippedCount, failedCount));
            });
        });
        return null;
    }

    private Component installModule(ServerCommandContext context, String[] args)
    {
        context.checkPermission(context.sender(), "plex.modules.install");
        String moduleName = args[2];
        plugin.getApi().scheduler().runAsync(() -> plugin.getUpdateChecker().installModuleJar(context.sender(), moduleName));
        return context.mmString("<green>Installing module <yellow>" + moduleName + "<green>...");
    }

    private Component uninstallModule(ServerCommandContext context, String[] args)
    {
        context.checkPermission(context.sender(), "plex.modules.uninstall");
        String moduleName = args[2];
        boolean removeData = args.length == 4;
        return switch (plugin.getModuleManager().uninstallModule(moduleName, removeData))
        {
            case NOT_FOUND -> context.mmString("<red>No installed module named <yellow>" + moduleName + "<red> was found.");
            case FAILED -> context.mmString("<red>Failed to delete the JAR for <yellow>" + moduleName + "<red>. Check the server log.");
            case REMOVED ->
            {
                context.send(context.sender(), context.mmString("<green>Uninstalled module <yellow>" + moduleName + "<green>" + (removeData ? " and its data folder" : "") + "."));
                yield context.messageComponent("moduleRestartRequired");
            }
        };
    }

    private Component update(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.update");
        plugin.getApi().scheduler().runAsync(() ->
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            if (result.status() == UpdateChecker.UpdateCheckStatus.UPDATE_AVAILABLE)
            {
                plugin.getUpdateChecker().updateJar(sender, result.metadata(), () -> sender.sendMessage(context.mmString("<red>Alert: Restart the server for the new JAR file to be applied.")));
            }
            else if (result.status() == UpdateChecker.UpdateCheckStatus.UP_TO_DATE)
            {
                sender.sendMessage(context.mmString("<red>Plex is already up to date!"));
            }
            else
            {
                plugin.getUpdateChecker().sendResultMessage(sender, result, 2);
            }
        });
        return null;
    }

}
