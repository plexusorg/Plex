package dev.plex.command;

import com.google.common.collect.Lists;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.Plex;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.Locale;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Brigadier-backed superclass for Plex's built-in server commands.
 */
public abstract class ServerCommand implements PlexCommand
{
    private static Runtime runtime;

    protected final Plex plugin;
    private final RequiredCommandSource commandSource;

    public static void setRuntime(Runtime runtime)
    {
        ServerCommand.runtime = runtime;
    }

    protected ServerCommand()
    {
        this.plugin = requireRuntime().plugin();
        this.commandSource = permissions().source();
    }

    protected abstract Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args);

    @Override
    public final LiteralCommandNode<CommandSourceStack> buildCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(getName())
                .requires(this::canUse);
        buildCommand(command);
        return command.build();
    }

    protected abstract void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command);

    protected LiteralArgumentBuilder<CommandSourceStack> literal(String literal)
    {
        return Commands.literal(literal);
    }

    protected RequiredArgumentBuilder<CommandSourceStack, String> word(String name)
    {
        return Commands.argument(name, StringArgumentType.word());
    }

    protected RequiredArgumentBuilder<CommandSourceStack, String> playerArgument(String name)
    {
        return word(name).suggests(suggestPlayers());
    }

    protected RequiredArgumentBuilder<CommandSourceStack, String> greedyString(String name)
    {
        return Commands.argument(name, StringArgumentType.greedyString());
    }

    protected RequiredArgumentBuilder<CommandSourceStack, Integer> nonNegativeInteger(String name)
    {
        return Commands.argument(name, IntegerArgumentType.integer(0));
    }

    protected int executeCommand(CommandContext<CommandSourceStack> context, String... args)
    {
        return dispatchCommand(context, args);
    }

    protected String string(CommandContext<CommandSourceStack> context, String name)
    {
        return StringArgumentType.getString(context, name);
    }

    protected int integer(CommandContext<CommandSourceStack> context, String name)
    {
        return IntegerArgumentType.getInteger(context, name);
    }

    protected String[] argsWithGreedy(String greedy)
    {
        return splitExecutionArgs(greedy);
    }

    protected String[] argsWithGreedy(String first, String greedy)
    {
        String[] greedyArgs = argsWithGreedy(greedy);
        String[] args = new String[greedyArgs.length + 1];
        args[0] = first;
        System.arraycopy(greedyArgs, 0, args, 1, greedyArgs.length);
        return args;
    }

    protected String[] argsWithGreedy(String first, String second, String greedy)
    {
        String[] greedyArgs = argsWithGreedy(greedy);
        String[] args = new String[greedyArgs.length + 2];
        args[0] = first;
        args[1] = second;
        System.arraycopy(greedyArgs, 0, args, 2, greedyArgs.length);
        return args;
    }

    protected SuggestionProvider<CommandSourceStack> suggest(Supplier<Collection<String>> suggestions)
    {
        return (context, builder) -> suggestMatching(builder, suggestions.get());
    }

    protected SuggestionProvider<CommandSourceStack> suggest(Collection<String> suggestions)
    {
        return (context, builder) -> suggestMatching(builder, suggestions);
    }

    protected SuggestionProvider<CommandSourceStack> suggestGreedyWords(Supplier<Collection<String>> suggestions)
    {
        return (context, builder) -> suggestLastGreedyToken(builder, suggestions.get());
    }

    protected SuggestionProvider<CommandSourceStack> suggestGreedyWords(Collection<String> suggestions)
    {
        return (context, builder) -> suggestLastGreedyToken(builder, suggestions);
    }

    protected CompletableFuture<Suggestions> suggestMatching(SuggestionsBuilder builder, Collection<String> suggestions)
    {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        for (String suggestion : suggestions)
        {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(remaining))
            {
                builder.suggest(suggestion);
            }
        }
        return builder.buildFuture();
    }

    protected CompletableFuture<Suggestions> suggestLastGreedyToken(SuggestionsBuilder builder, Collection<String> suggestions)
    {
        String remaining = builder.getRemaining();
        int tokenStart = remaining.lastIndexOf(' ') + 1;
        SuggestionsBuilder tokenBuilder = tokenStart == 0 ? builder : builder.createOffset(builder.getStart() + tokenStart);
        return suggestMatching(tokenBuilder, suggestions);
    }

    protected CompletableFuture<Suggestions> suggestOptionalFlags(SuggestionsBuilder builder, Collection<String> flags)
    {
        String remaining = builder.getRemaining();
        if (remaining.isBlank())
        {
            return builder.buildFuture();
        }

        List<String> availableFlags = Lists.newArrayList(flags);
        for (String token : remaining.split("\\s+"))
        {
            if (token.isBlank())
            {
                continue;
            }
            if (flags.stream().anyMatch(flag -> flag.equalsIgnoreCase(token)))
            {
                availableFlags.removeIf(flag -> flag.equalsIgnoreCase(token));
            }
        }

        String currentToken = remaining.substring(remaining.lastIndexOf(' ') + 1);
        if (!currentToken.startsWith("-"))
        {
            return builder.buildFuture();
        }
        return suggestLastGreedyToken(builder, availableFlags);
    }

    protected SuggestionProvider<CommandSourceStack> suggestPlayers()
    {
        return suggest(PlexUtils::getPlayerNameList);
    }

    protected SuggestionProvider<CommandSourceStack> suggestPlayersAndAll()
    {
        return suggest(() ->
        {
            List<String> suggestions = Lists.newArrayList(PlexUtils.getPlayerNameList());
            suggestions.add("-a");
            return suggestions;
        });
    }

    protected SuggestionProvider<CommandSourceStack> suggestPlayersAndAll(String permission)
    {
        return (context, builder) ->
        {
            if (!silentCheckPermission(context.getSource().getSender(), permission))
            {
                return builder.buildFuture();
            }
            List<String> suggestions = Lists.newArrayList(PlexUtils.getPlayerNameList());
            suggestions.add("-a");
            return suggestMatching(builder, suggestions);
        };
    }

    private boolean canUse(CommandSourceStack source)
    {
        CommandSender sender = source.getSender();
        if (commandSource == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            return false;
        }

        if (commandSource == RequiredCommandSource.IN_GAME && sender instanceof ConsoleCommandSender)
        {
            return false;
        }

        String permission = getPermission();
        if (permission.isEmpty())
        {
            return !(sender instanceof Player player) || hasCachedPlexPlayer(player);
        }

        if (sender instanceof Player player)
        {
            return hasCachedPlexPlayer(player) && player.hasPermission(permission);
        }

        if (sender instanceof ConsoleCommandSender && !sender.getName().equalsIgnoreCase("console"))
        {
            PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(sender.getName());
            Player player = plexPlayer == null ? null : Bukkit.getPlayer(plexPlayer.getName());
            return player != null && plugin.getPermissions().playerHas(null, player, permission);
        }

        return true;
    }

    private int dispatchCommand(CommandContext<CommandSourceStack> context, String[] args)
    {
        CommandSender sender = context.getSource().getSender();
        if (!validateSourceAndPermission(sender))
        {
            return com.mojang.brigadier.Command.SINGLE_SUCCESS;
        }

        try
        {
            Component component = this.execute(sender, isConsole(sender) ? null : (Player)sender, args);
            if (component != null)
            {
                send(sender, component);
            }
        }
        catch (PlayerNotFoundException | CommandFailException | ConsoleOnlyException |
               ConsoleMustDefinePlayerException | PlayerNotBannedException | NumberFormatException ex)
        {
            send(sender, exceptionComponent(ex));
        }
        return com.mojang.brigadier.Command.SINGLE_SUCCESS;
    }

    private boolean validateSourceAndPermission(CommandSender sender)
    {
        if (commandSource == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            send(sender, messageComponent("noPermissionInGame"));
            return false;
        }

        if (commandSource == RequiredCommandSource.IN_GAME && sender instanceof ConsoleCommandSender)
        {
            send(sender, messageComponent("noPermissionConsole"));
            return false;
        }

        String permission = getPermission();
        if (permission.isEmpty())
        {
            return true;
        }

        if (sender instanceof Player player)
        {
            if (!hasCachedPlexPlayer(player))
            {
                return false;
            }
            if (!player.hasPermission(permission))
            {
                send(sender, messageComponent("noPermissionNode", permission));
                return false;
            }
            return true;
        }

        if (sender instanceof ConsoleCommandSender && !sender.getName().equalsIgnoreCase("console"))
        {
            PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(sender.getName());
            Player player = plexPlayer == null ? null : Bukkit.getPlayer(plexPlayer.getName());
            if (player == null || !plugin.getPermissions().playerHas(null, player, permission))
            {
                send(sender, messageComponent("noPermissionNode", permission));
                return false;
            }
        }

        return true;
    }

    private boolean hasCachedPlexPlayer(Player player)
    {
        return plugin.getPlayerCache().getPlexPlayerMap().containsKey(player.getUniqueId());
    }

    private String[] splitExecutionArgs(String rawArgs)
    {
        if (rawArgs.isBlank())
        {
            return new String[0];
        }
        return rawArgs.trim().split("\\s+");
    }

    protected PlexPlayer getPlexPlayer(@NotNull Player player)
    {
        return plugin.getPlayerService().getPlayer(player.getUniqueId());
    }

    protected void send(Audience audience, String s)
    {
        audience.sendMessage(componentFromString(s));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    protected boolean checkPermission(CommandSender sender, String permission)
    {
        if (!isConsole(sender))
        {
            return checkPermission((Player)sender, permission);
        }
        return true;
    }

    protected boolean silentCheckPermission(CommandSender sender, String permission)
    {
        PlexLog.debug("Checking {0} with {1}", sender.getName(), permission);
        if (!isConsole(sender))
        {
            return silentCheckPermission((Player)sender, permission);
        }
        return true;
    }

    protected boolean checkPermission(Player player, String permission)
    {
        if (!permission.isEmpty() && !player.hasPermission(permission))
        {
            throw new CommandFailException(PlexUtils.messageString("noPermissionNode", permission));
        }
        return true;
    }

    protected boolean silentCheckPermission(Player player, String permission)
    {
        return permission.isEmpty() || player.hasPermission(permission);
    }

    protected UUID getUUID(CommandSender sender)
    {
        if (!(sender instanceof Player player))
        {
            return null;
        }
        return player.getUniqueId();
    }

    public @NotNull Plex getPlugin()
    {
        return plugin;
    }

    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    protected Component messageComponent(String s, Object... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    protected Component messageComponent(String s, Component... objects)
    {
        return PlexUtils.messageComponent(s, objects);
    }

    protected String messageString(String s, Object... objects)
    {
        return PlexUtils.messageString(s, objects);
    }

    protected Component usage()
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(this.getUsage()).color(NamedTextColor.GRAY));
    }

    protected Component usage(String s)
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(s).color(NamedTextColor.GRAY));
    }

    private Component exceptionComponent(RuntimeException ex)
    {
        if (ex instanceof PlayerNotFoundException && "PlayerNotFoundException".equals(ex.getMessage()))
        {
            return messageComponent("playerNotFound");
        }
        if (ex instanceof PlayerNotBannedException && "PlayerNotBannedException".equals(ex.getMessage()))
        {
            return messageComponent("playerNotBanned");
        }
        if (ex instanceof ConsoleOnlyException && "ConsoleOnlyException".equals(ex.getMessage()))
        {
            return messageComponent("consoleOnly");
        }
        if (ex instanceof ConsoleMustDefinePlayerException && "ConsoleMustDefinePlayerException".equals(ex.getMessage()))
        {
            return messageComponent("consoleMustDefinePlayer");
        }
        String message = ex.getMessage();
        return message == null ? componentFromString(ex.getClass().getSimpleName()) : PlexUtils.mmDeserialize(message);
    }

    protected Player getNonNullPlayer(String name)
    {
        try
        {
            UUID uuid = UUID.fromString(name);
            return Bukkit.getPlayer(uuid);
        }
        catch (IllegalArgumentException ignored)
        {
        }

        Player player = Bukkit.getPlayer(name);
        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        return player;
    }

    protected PlexPlayer getOnlinePlexPlayer(String name)
    {
        Player player = getNonNullPlayer(name);
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(player.getUniqueId());
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    protected PlexPlayer getOfflinePlexPlayer(UUID uuid)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(uuid);
        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        return plexPlayer;
    }

    protected World getNonNullWorld(String name)
    {
        World world = Bukkit.getWorld(name);
        if (world == null)
        {
            throw new CommandFailException(PlexUtils.messageString("worldNotFound"));
        }
        return world;
    }

    protected Component componentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s).colorIfAbsent(NamedTextColor.GRAY);
    }

    protected Component noColorComponentFromString(String s)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(s);
    }

    protected Component mmString(String s)
    {
        return PlexUtils.mmDeserialize(s);
    }

    private static Runtime requireRuntime()
    {
        if (runtime == null)
        {
            throw new IllegalStateException("ServerCommand runtime has not been installed by Plex");
        }
        return runtime;
    }

    public interface Runtime
    {
        Plex plugin();
    }
}
