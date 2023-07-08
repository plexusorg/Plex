package dev.plex.command;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.command.annotation.CommandName;
import dev.plex.command.annotation.CommandPermission;
import dev.plex.command.annotation.Default;
import dev.plex.command.annotation.SubCommand;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.ReflectionsUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Taah
 * @project Plex
 * @since 2:27 PM [07-07-2023]
 */
public abstract class PlexBrigadierCommand
{
    protected final Plex plugin;
    private CommandDispatcher<BukkitBrigadierCommandSource> commandDispatcher;

    public PlexBrigadierCommand()
    {
        this.plugin = Plex.get();
        try
        {
            final Object dedicatedServer = ReflectionsUtil.callFunction(getCraftServer(), "getServer");
            final Object minecraftServer = Class.forName("net.minecraft.server.MinecraftServer").cast(dedicatedServer);
            //            System.out.println(Arrays.toString(Arrays.stream(minecraftServer.getClass().getDeclaredMethods()).map(Method::getName).toArray(String[]::new)));

            final Object serverFunctionsManager = ReflectionsUtil.callFunction(minecraftServer, "aA");
            this.commandDispatcher = ReflectionsUtil.callFunction(serverFunctionsManager, "b");
        }
        catch (ClassNotFoundException e)
        {
            this.commandDispatcher = null;
            PlexLog.error("Disabling commands as brigadier could not properly be located.");
        }

        if (!this.getClass().isAnnotationPresent(CommandName.class))
        {
            PlexLog.error("Cannot find command name for class " + this.getClass().getName());
            return;
        }

        String[] commandName = this.getClass().getAnnotation(CommandName.class).value();

        final HashMap<String, Method> subcommands = Maps.newHashMap();

        Method defaultMethod = null;
        for (Method declaredMethod : this.getClass().getDeclaredMethods())
        {
            if (declaredMethod.isAnnotationPresent(SubCommand.class))
            {
                String subcommand = declaredMethod.getAnnotation(SubCommand.class).value();
                subcommands.put(subcommand.toLowerCase(), declaredMethod);
            }
            if (declaredMethod.isAnnotationPresent(Default.class))
            {
                if (defaultMethod != null)
                {
                    PlexLog.error("There cannot be more than one default execution.");
                    continue;
                }
                defaultMethod = declaredMethod;
            }
        }

        if (this.commandDispatcher != null)
        {
            for (String s : commandName)
            {
                PlexLog.debug("registering command " + s);
                LiteralArgumentBuilder<BukkitBrigadierCommandSource> builder = LiteralArgumentBuilder.literal(s.toLowerCase());
                for (Map.Entry<String, Method> stringMethodEntry : subcommands.entrySet())
                {
                    PlexLog.debug("registering subcommand " + stringMethodEntry.getKey());
                    String[] subCommandArgs = stringMethodEntry.getKey().split(" ");
                    LiteralArgumentBuilder<BukkitBrigadierCommandSource> parentBuilder = LiteralArgumentBuilder.literal(subCommandArgs[0]);
                    LiteralArgumentBuilder<BukkitBrigadierCommandSource> currSubCommand = parentBuilder;
                    if (subCommandArgs.length == 1)
                    {
                        parentBuilder.executes(context ->
                        {
                            if (stringMethodEntry.getValue().isAnnotationPresent(CommandPermission.class))
                            {
                                String permission = stringMethodEntry.getValue().getAnnotation(CommandPermission.class).value();
                                if (!context.getSource().getBukkitSender().hasPermission(permission))
                                {
                                    send(context, PlexUtils.messageString("noPermissionNode", permission));
                                    return 0;
                                }
                            }
                            try
                            {
                                stringMethodEntry.getValue().invoke(this, context.getSource().getBukkitSender());
                            }
                            catch (Exception e)
                            {
                                PlexLog.error(e.getMessage());
                                for (StackTraceElement stackTraceElement : e.getStackTrace())
                                {
                                    PlexLog.error(stackTraceElement.toString());
                                }
                                return 0;
                            }
                            return 1;
                        });
                    }
                    else
                    {
                        for (int i = 1; i < subCommandArgs.length; i++)
                        {
                            LiteralArgumentBuilder<BukkitBrigadierCommandSource> curr = LiteralArgumentBuilder.literal(subCommandArgs[i]);
                            if (i == subCommandArgs.length - 1)
                            {
                                curr.executes(context ->
                                {
                                    if (stringMethodEntry.getValue().isAnnotationPresent(CommandPermission.class))
                                    {
                                        String permission = stringMethodEntry.getValue().getAnnotation(CommandPermission.class).value();
                                        if (!context.getSource().getBukkitSender().hasPermission(permission))
                                        {
                                            send(context, PlexUtils.messageString("noPermissionNode", permission));
                                            return 0;
                                        }
                                    }
                                    try
                                    {
                                        stringMethodEntry.getValue().invoke(this, context.getSource().getBukkitSender());
                                    }
                                    catch (Exception e)
                                    {
                                        PlexLog.error(e.getMessage());
                                        for (StackTraceElement stackTraceElement : e.getStackTrace())
                                        {
                                            PlexLog.error(stackTraceElement.toString());
                                        }
                                        return 0;
                                    }
                                    return 1;
                                });
                            }
                            currSubCommand.then(curr);
                            currSubCommand = curr;
                        }
                    }

                    PlexLog.debug(new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(parentBuilder));

                    builder = builder.then(
                            parentBuilder
                    );
                }
                if (defaultMethod != null)
                {
                    PlexLog.debug("registering default method");
                    Method finalDefaultMethod = defaultMethod;
                    finalDefaultMethod.setAccessible(true);
                    builder = builder.executes(context ->
                    {
                        if (finalDefaultMethod.isAnnotationPresent(CommandPermission.class))
                        {
                            String permission = finalDefaultMethod.getAnnotation(CommandPermission.class).value();
                            if (!context.getSource().getBukkitSender().hasPermission(permission))
                            {
                                send(context, PlexUtils.messageString("noPermissionNode", permission));
                                return 0;
                            }
                        }
                        try
                        {
                            finalDefaultMethod.invoke(this, context.getSource().getBukkitSender());
                        }
                        catch (Exception e)
                        {
                            PlexLog.error(e.getMessage());
                            for (StackTraceElement stackTraceElement : e.getStackTrace())
                            {
                                PlexLog.error(stackTraceElement.toString());
                            }
                            return 0;
                        }
                        return 1;
                    });
                }
                PlexLog.debug(new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create().toJson(builder));
                this.commandDispatcher.register(builder);
            }
        }
    }

    /**
     * Gets a PlexPlayer from Player object
     *
     * @param player The player object
     * @return PlexPlayer Object
     * @see PlexPlayer
     */
    protected PlexPlayer getPlexPlayer(@NotNull Player player)
    {
        return DataUtils.getPlayer(player.getUniqueId());
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    /**
     * Sends a message to an Audience
     *
     * @param audience The Audience to send the message to
     * @param s        The message to send
     */
    protected void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    /**
     * Sends a message to a CommandSender
     *
     * @param context The Command Context's sender to send the message to
     * @param s       The message to send
     */
    protected void send(CommandContext<BukkitBrigadierCommandSource> context, String s)
    {
        context.getSource().getBukkitSender().sendMessage(componentFromString(s));
    }

    /**
     * Sends a message to a CommandSender
     *
     * @param context   The Command Context's sender to send the message to
     * @param component The Component to send
     */
    protected void send(CommandContext<BukkitBrigadierCommandSource> context, Component component)
    {
        context.getSource().getBukkitSender().sendMessage(component);
    }

    /**
     * Converts a String to a MiniMessage Component
     *
     * @param s The String to convert
     * @return A Kyori Component
     */
    protected Component mmString(String s)
    {
        return PlexUtils.mmDeserialize(s);
    }

    /**
     * Converts a String to a legacy Kyori Component
     *
     * @param s The String to convert
     * @return A Kyori component
     */
    protected Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    /**
     * Checks if a player is an admin
     *
     * @param plexPlayer The PlexPlayer object
     * @return true if the player is an admin
     * @see PlexPlayer
     */
    protected boolean isAdmin(PlexPlayer plexPlayer)
    {
        return Plex.get().getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a sender is an admin
     *
     * @param sender A command sender
     * @return true if the sender is an admin or if console
     */
    protected boolean isAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return true;
        }
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return plugin.getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a username is an admin
     *
     * @param name The username
     * @return true if the username is an admin
     */
    protected boolean isAdmin(String name)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(name);
        return plugin.getRankManager().isAdmin(plexPlayer);
    }

    /**
     * Checks if a sender is a senior admin
     *
     * @param sender A command sender
     * @return true if the sender is a senior admin or if console
     */
    protected boolean isSeniorAdmin(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return true;
        }
        PlexPlayer plexPlayer = getPlexPlayer(player);
        return plugin.getRankManager().isSeniorAdmin(plexPlayer);
    }

    /**
     * Gets the UUID of the sender
     *
     * @param sender A command sender
     * @return A unique ID or null if the sender is console
     * @see UUID
     */
    protected UUID getUUID(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return null;
        }
        return player.getUniqueId();
    }

    private Object getCraftServer()
    {
        String nmsVersion = Bukkit.getServer().getClass().getPackage().getName();
        nmsVersion = nmsVersion.substring(nmsVersion.lastIndexOf('.') + 1);
        try
        {
            Class<?> craftServer = Class.forName("org.bukkit.craftbukkit." + nmsVersion + ".CraftServer");
            return craftServer.cast(Bukkit.getServer());
        }
        catch (ClassNotFoundException e)
        {
            throw new RuntimeException(e);
        }
    }
}
