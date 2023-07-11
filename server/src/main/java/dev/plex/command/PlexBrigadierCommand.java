package dev.plex.command;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.command.annotation.*;
import dev.plex.command.source.RequiredCommandSource;
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

import java.lang.System;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

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
            if (this.commandDispatcher != null)
            {
                this.commandDispatcher.register(execute());
            }
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
            for (String name : commandName)
            {
                LiteralArgumentBuilder<BukkitBrigadierCommandSource> builder = LiteralArgumentBuilder.literal(name.toLowerCase());

                for (Map.Entry<String, Method> stringMethodEntry : subcommands.entrySet())
                {
                    String[] subCommandArgs = stringMethodEntry.getKey().split(" ");
                    LinkedList<LiteralArgumentBuilder<BukkitBrigadierCommandSource>> builders = new LinkedList<>();
                    for (int i = 0; i < subCommandArgs.length; i++)
                    {
                        LiteralArgumentBuilder<BukkitBrigadierCommandSource> newNode = LiteralArgumentBuilder.literal(subCommandArgs[i]);
                        builders.addLast(newNode);
                    }

                    if (builders.size() == 1)
                    {
                        LiteralArgumentBuilder<BukkitBrigadierCommandSource> parent = builders.removeFirst();
                        LinkedList<RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> argumentBuilders = new LinkedList<>();

                        LinkedHashMap<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> arguments = getArguments(stringMethodEntry.getValue());
                        for (Map.Entry<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> parameterArgumentBuilderEntry : arguments.entrySet())
                        {
                            argumentBuilders.addLast(parameterArgumentBuilderEntry.getValue());
                        }
                        boolean setExecution = false;
                        CommandNode<BukkitBrigadierCommandSource> parentArg = null;
                        CommandNode<BukkitBrigadierCommandSource> currArg = null;
                        while (!argumentBuilders.isEmpty())
                        {
                            if (parentArg == null)
                            {
                                RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newParent = argumentBuilders.removeFirst();
                                if (argumentBuilders.isEmpty())
                                {
                                    newParent.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                                    setExecution = true;
                                }
                                parentArg = newParent.build();
                                parent.then(parentArg);
                                currArg = parentArg;
                            }
                            else
                            {
                                RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newCurr = argumentBuilders.removeFirst();
                                if (argumentBuilders.isEmpty())
                                {
                                    newCurr.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                                    setExecution = true;
                                }
                                CommandNode<BukkitBrigadierCommandSource> newCurrNode = newCurr.build();
                                currArg.addChild(newCurrNode);
                                currArg = newCurrNode;
                            }
                        }
                        if (!setExecution)
                        {
                            parent.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                        }
                        builder.then(parent);
                    }
                    else if (builders.size() > 1)
                    {
                        LiteralCommandNode<BukkitBrigadierCommandSource> parent = builders.removeFirst().build();
                        LiteralCommandNode<BukkitBrigadierCommandSource> curr = null;
                        while (!builders.isEmpty())
                        {
                            LiteralArgumentBuilder<BukkitBrigadierCommandSource> newCurr = builders.removeFirst();
                            PlexLog.debug("Adding subcommand " + newCurr.getLiteral());
                            if (builders.isEmpty())
                            {
                                LinkedList<RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> argumentBuilders = new LinkedList<>();
                                LinkedHashMap<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> arguments = getArguments(stringMethodEntry.getValue());
                                for (Map.Entry<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> parameterArgumentBuilderEntry : arguments.entrySet())
                                {
                                    argumentBuilders.addLast(parameterArgumentBuilderEntry.getValue());
                                }
                                boolean setExecution = false;
                                CommandNode<BukkitBrigadierCommandSource> parentArg = null;
                                CommandNode<BukkitBrigadierCommandSource> currArg = null;
                                while (!argumentBuilders.isEmpty())
                                {
                                    if (parentArg == null)
                                    {
                                        RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newParent = argumentBuilders.removeFirst();
                                        if (argumentBuilders.isEmpty())
                                        {
                                            newParent.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                                            setExecution = true;
                                        }
                                        parentArg = newParent.build();
                                        newCurr.then(parentArg);
                                        currArg = parentArg;
                                    }
                                    else
                                    {
                                        RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newCurrArg = argumentBuilders.removeFirst();
                                        if (argumentBuilders.isEmpty())
                                        {
                                            newCurrArg.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                                            setExecution = true;
                                        }
                                        CommandNode<BukkitBrigadierCommandSource> newCurrNode = newCurrArg.build();
                                        currArg.addChild(newCurrNode);
                                        currArg = newCurrNode;
                                    }
                                }
                                if (!setExecution)
                                {
                                    newCurr.executes(context -> execute(stringMethodEntry.getValue(), context, arguments.keySet()));
                                }
                            }
                            if (curr == null)
                            {
                                LiteralCommandNode<BukkitBrigadierCommandSource> temp = newCurr.build();
                                parent.addChild(temp);
                                curr = temp;
                            }
                            else
                            {
                                LiteralCommandNode<BukkitBrigadierCommandSource> temp = newCurr.build();
                                curr.addChild(temp);
                                curr = temp;
                            }
                        }
                        builder.then(parent);
                    }
                    PlexLog.debug("Overall Builder: " + new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(builder));
                }

                if (defaultMethod != null)
                {
                    LinkedList<RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> argumentBuilders = new LinkedList<>();
                    LinkedHashMap<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> arguments = getArguments(defaultMethod);
                    for (Map.Entry<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> parameterArgumentBuilderEntry : arguments.entrySet())
                    {
                        argumentBuilders.addLast(parameterArgumentBuilderEntry.getValue());
                    }
                    boolean setExecution = false;
                    CommandNode<BukkitBrigadierCommandSource> parentArg = null;
                    CommandNode<BukkitBrigadierCommandSource> currArg = null;
                    while (!argumentBuilders.isEmpty())
                    {
                        if (parentArg == null)
                        {
                            RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newParent = argumentBuilders.removeFirst();
                            if (argumentBuilders.isEmpty())
                            {
                                Method finalDefaultMethod = defaultMethod;
                                newParent.executes(context -> execute(finalDefaultMethod, context, arguments.keySet()));
                                setExecution = true;
                            }
                            parentArg = newParent.build();
                            builder.then(parentArg);
                            currArg = parentArg;
                        }
                        else
                        {
                            RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?> newCurrArg = argumentBuilders.removeFirst();
                            if (argumentBuilders.isEmpty())
                            {
                                Method finalDefaultMethod1 = defaultMethod;
                                newCurrArg.executes(context -> execute(finalDefaultMethod1, context, arguments.keySet()));
                                setExecution = true;
                            }
                            CommandNode<BukkitBrigadierCommandSource> newCurrNode = newCurrArg.build();
                            currArg.addChild(newCurrNode);
                            currArg = newCurrNode;
                        }
                    }
                    if (!setExecution)
                    {
                        Method finalDefaultMethod2 = defaultMethod;
                        builder.executes(context -> execute(finalDefaultMethod2, context, arguments.keySet()));
                    }
                }

                this.commandDispatcher.register(builder);
            }

            this.commandDispatcher.register(LiteralArgumentBuilder.<BukkitBrigadierCommandSource>literal("testing")
                    .then(RequiredArgumentBuilder.<BukkitBrigadierCommandSource, Integer>argument("test0", IntegerArgumentType.integer())
                            .then(RequiredArgumentBuilder.<BukkitBrigadierCommandSource, String>argument("test", StringArgumentType.word())
                                    .then(RequiredArgumentBuilder.<BukkitBrigadierCommandSource, String>argument("test1", StringArgumentType.word())
                                            .executes(context ->
                                            {
                                                send(context, context.getArgument("test", String.class));
                                                send(context, context.getArgument("test1", String.class));
                                                return 1;
                                            })))));
        }
    }

    public LiteralArgumentBuilder<BukkitBrigadierCommandSource> execute()
    {
        return LiteralArgumentBuilder.literal(this.getClass().getName().toLowerCase());
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

    private LinkedHashMap<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> getArguments(Method method)
    {
        LinkedHashMap<Parameter, RequiredArgumentBuilder<BukkitBrigadierCommandSource, ?>> result = new LinkedHashMap<>();
        if (!method.canAccess(this))
        {
            method.setAccessible(true);
        }
        for (Parameter parameter : method.getParameters())
        {
            if (parameter.isAnnotationPresent(Argument.class))
            {
                Argument argument = parameter.getAnnotation(Argument.class);
                if (String.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), argument.argumentType() == StringArgumentType.StringType.SINGLE_WORD ? StringArgumentType.word() : StringArgumentType.greedyString()));
                }
                else if (int.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), IntegerArgumentType.integer(argument.min() == Double.MIN_VALUE ? Integer.MIN_VALUE : (int) argument.min(), argument.max() == Double.MAX_VALUE ? Integer.MAX_VALUE : (int) argument.max())));
                }
                else if (double.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), DoubleArgumentType.doubleArg(argument.min(), argument.max())));
                }
                else if (float.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), FloatArgumentType.floatArg(argument.min() == Double.MIN_VALUE ? Float.MIN_VALUE : (int) argument.min(), argument.max() == Double.MAX_VALUE ? Float.MAX_VALUE : (int) argument.max())));
                }
                else if (boolean.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), BoolArgumentType.bool()));
                }
                else if (long.class.isAssignableFrom(parameter.getType()))
                {
                    result.put(parameter, RequiredArgumentBuilder.argument(argument.value(), LongArgumentType.longArg(argument.min() == Double.MIN_VALUE ? Long.MIN_VALUE : (int) argument.min(), argument.max() == Double.MAX_VALUE ? Long.MAX_VALUE : (int) argument.max())));
                }
            }
        }
        return result;
    }

    private Object getArgument(Class<?> clazz, CommandContext<BukkitBrigadierCommandSource> context, String name)
    {
        if (String.class.isAssignableFrom(clazz))
        {
            return StringArgumentType.getString(context, name);
        }
        else if (int.class.isAssignableFrom(clazz))
        {
            return IntegerArgumentType.getInteger(context, name);
        }
        else if (double.class.isAssignableFrom(clazz))
        {
            return DoubleArgumentType.getDouble(context, name);
        }
        else if (float.class.isAssignableFrom(clazz))
        {
            return FloatArgumentType.getFloat(context, name);
        }
        else if (boolean.class.isAssignableFrom(clazz))
        {
            return BoolArgumentType.getBool(context, name);
        }
        else if (long.class.isAssignableFrom(clazz))
        {
            return LongArgumentType.getLong(context, name);
        }
        return null;
    }

    private int execute(Method method, CommandContext<BukkitBrigadierCommandSource> context, Set<Parameter> arguments)
    {
        if (method.isAnnotationPresent(CommandPermission.class))
        {
            String permission = method.getAnnotation(CommandPermission.class).value();
            if (!context.getSource().getBukkitSender().hasPermission(permission))
            {
                send(context, PlexUtils.messageComponent("noPermissionNode", permission));
                return 1;
            }
        }
        try
        {
            List<Object> params = arguments
                    .stream().map(bukkitBrigadierCommandSourceArgumentBuilder -> getArgument(bukkitBrigadierCommandSourceArgumentBuilder.getType(), context, bukkitBrigadierCommandSourceArgumentBuilder.getAnnotation(Argument.class).value())).toList();
            LinkedList<Object> parameters = new LinkedList<>(params);
//            parameters.addFirst(context.getSource().getBukkitSender());
            if (method.isAnnotationPresent(CommandSource.class)) {
                RequiredCommandSource commandSource = method.getAnnotation(CommandSource.class).value();
                if (commandSource == RequiredCommandSource.IN_GAME) {
                    if (!(context.getSource().getBukkitSender() instanceof Player player)) {
                        send(context, PlexUtils.messageComponent("noPermissionConsole"));
                        return 1;
                    } else {
                        parameters.addFirst(player);
                    }
                } else if (commandSource == RequiredCommandSource.CONSOLE) {
                    if (context.getSource().getBukkitSender() instanceof Player) {
                        send(context, PlexUtils.messageComponent("noPermissionInGame"));
                        return 1;
                    }
                    parameters.addFirst(context.getSource().getBukkitSender());
                } else {
                    parameters.addFirst(context.getSource().getBukkitSender());
                }
            }
            System.out.println(Arrays.toString(parameters.stream().map(Object::getClass).map(Class::getName).toArray()));
            System.out.println(Arrays.toString(Arrays.stream(method.getParameterTypes()).map(Class::getName).toArray()));
            method.invoke(this, parameters.toArray());
            return 1;
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
