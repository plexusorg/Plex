package dev.plex.command.impl;

import org.bukkit.Bukkit;


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
        command.executes(context -> executeCommand(context, this::information));
        command.then(literal("reload").executes(context -> executeCommand(context, this::reload)));
        command.then(literal("update").executes(context -> executeCommand(context, this::update)));

        LiteralArgumentBuilder<CommandSourceStack> modules = literal("modules")
                .executes(context -> executeCommand(context, this::modules));
        modules.then(literal("reload").executes(context -> executeCommand(context, this::reloadModules)));
        modules.then(literal("update").executes(context -> executeCommand(context, this::updateModules)));
        modules.then(literal("install").then(word("name").executes(context -> executeCommand(context,
                commandContext -> installModule(commandContext, string(context, "name"))))));
        modules.then(literal("uninstall").then(word("name")
                .suggests(suggest(() -> plugin.getModuleManager().getModules().stream()
                        .map(module -> module.getPlexModuleFile().getName()).collect(Collectors.toList())))
                .executes(context -> executeCommand(context, commandContext ->
                        uninstallModule(commandContext, string(context, "name"), false)))
                .then(literal("-rmdir").executes(context -> executeCommand(context, commandContext ->
                        uninstallModule(commandContext, string(context, "name"), true))))));
        command.then(modules);
    }

    private Component information(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
            sender.sendMessage(PlexUtils.mmDeserialize("<light_purple>Plex - A new freedom plugin."));
            sender.sendMessage(PlexUtils.mmDeserialize("<light_purple>Plugin version: <gold>" + plugin.getPluginMeta().getVersion() + " #" + BuildInfo.getNumber() + " <light_purple>Git: <gold>" + BuildInfo.shortenCommit(BuildInfo.getCommit())));
            sender.sendMessage(PlexUtils.mmDeserialize("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
            sender.sendMessage(PlexUtils.mmDeserialize("<light_purple>Built by: <gold>" + BuildInfo.getAuthor() + " <light_purple>on <gold>" + BuildInfo.getDate()));
            sender.sendMessage(PlexUtils.mmDeserialize("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
            plugin.getUpdateChecker().getUpdateStatusMessageAsync(sender, true, 2);
        return null;
    }

    private Component reload(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.reload");
        plugin.config.load();
        PlexLog.setDebugEnabled(plugin.config.getBoolean("debug"));
        sender.sendMessage("Reloaded config file");
        plugin.entities.load();
        sender.sendMessage("Reloaded entities file");
        plugin.worlds.load();
        sender.sendMessage("Reloaded worlds file");
        plugin.messages.load();
        PlexUtils.configure(plugin.config, plugin.messages);
        sender.sendMessage("Reloaded messages file");
        plugin.toggles.load();
        sender.sendMessage("Reloaded toggles file");
        plugin.indefBans.load(false);
        plugin.getPunishmentManager().mergeIndefiniteBans();
        sender.sendMessage("Reloaded indefinite bans");
        plugin.getServiceManager().endServices();
        plugin.getServiceManager().startServices();
        sender.sendMessage("Restarted services.");
        TimeUtils.TIMEZONE = plugin.config.getString("server.timezone", "Etc/UTC");
        sender.sendMessage("Set timezone to: " + TimeUtils.TIMEZONE);
        sender.sendMessage("Plex successfully reloaded.");
        return null;
    }

    private Component modules(ServerCommandContext context)
    {
        return PlexUtils.mmDeserialize("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
    }

    private Component reloadModules(ServerCommandContext context)
    {
        context.checkPermission(context.sender(), "plex.modules.reload");
        plugin.getModuleManager().reloadModules().whenComplete((ignored, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Failed to reload modules", failure);
                context.sender().sendMessage(PlexUtils.mmDeserialize("<red>Failed to reload modules. Check the server log."));
                return;
            }
            context.sender().sendMessage(PlexUtils.mmDeserialize("<green>All modules reloaded!"));
        });
        return null;
    }

    private Component updateModules(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.modules.update");
        java.util.concurrent.CompletableFuture.runAsync(() ->
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
            plugin.getModuleManager().reloadModules().whenComplete((ignored, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.error("Failed to reload updated modules", failure);
                }
                sender.sendMessage(PlexUtils.messageComponent("moduleUpdateSummary", updatedCount, skippedCount, failedCount));
            });
        }, plugin.getIoExecutor());
        return null;
    }

    private Component installModule(ServerCommandContext context, String moduleName)
    {
        context.checkPermission(context.sender(), "plex.modules.install");
        java.util.concurrent.CompletableFuture.runAsync(
                () -> plugin.getUpdateChecker().installModuleJar(context.sender(), moduleName), plugin.getIoExecutor());
        return PlexUtils.mmDeserialize("<green>Installing module <yellow>" + moduleName + "<green>...");
    }

    private Component uninstallModule(ServerCommandContext context, String moduleName, boolean removeData)
    {
        context.checkPermission(context.sender(), "plex.modules.uninstall");
        plugin.getModuleManager().uninstallModule(moduleName, removeData).whenComplete((result, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Failed to uninstall module " + moduleName, failure);
                context.sender().sendMessage(PlexUtils.mmDeserialize("<red>Failed to uninstall module <yellow>" + moduleName + "<red>. Check the server log."));
                return;
            }
            switch (result)
            {
                case NOT_FOUND -> context.sender().sendMessage(PlexUtils.mmDeserialize("<red>No installed module named <yellow>" + moduleName + "<red> was found."));
                case FAILED -> context.sender().sendMessage(PlexUtils.mmDeserialize("<red>Failed to delete the JAR for <yellow>" + moduleName + "<red>. Check the server log."));
                case REMOVED ->
                {
                    context.sender().sendMessage(PlexUtils.mmDeserialize("<green>Uninstalled module <yellow>" + moduleName + "<green>" + (removeData ? " and its data folder" : "") + "."));
                    context.sender().sendMessage(PlexUtils.messageComponent("moduleRestartRequired"));
                }
            }
        });
        return null;
    }

    private Component update(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        context.checkPermission(sender, "plex.update");
        java.util.concurrent.CompletableFuture.runAsync(() ->
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            if (result.status() == UpdateChecker.UpdateCheckStatus.UPDATE_AVAILABLE)
            {
                plugin.getUpdateChecker().updateJar(sender, result.metadata(), () -> sender.sendMessage(PlexUtils.mmDeserialize("<red>Alert: Restart the server for the new JAR file to be applied.")));
            }
            else if (result.status() == UpdateChecker.UpdateCheckStatus.UP_TO_DATE)
            {
                sender.sendMessage(PlexUtils.mmDeserialize("<red>Plex is already up to date!"));
            }
            else
            {
                plugin.getUpdateChecker().sendResultMessage(sender, result, 2);
            }
        }, plugin.getIoExecutor());
        return null;
    }

}
