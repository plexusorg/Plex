package dev.plex.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.plex.api.PlexApi;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.ConsoleMustDefinePlayerException;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotBannedException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.PlexModule;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Convenience base for module commands that execute from string-array arguments.
 *
 * <p>Commands that need a custom Brigadier tree can override
 * {@link #configureCommand(LiteralArgumentBuilder)} while keeping the same
 * metadata and helper methods.</p>
 */
public abstract class SimplePlexCommand implements PlexCommand
{
    private final CommandSpec commandSpec;
    private PlexApi api;
    private PlexModule module;

    /**
     * Creates a command using explicit command metadata.
     *
     * @param commandSpec command metadata
     */
    protected SimplePlexCommand(CommandSpec commandSpec)
    {
        this.commandSpec = commandSpec;
    }

    /**
     * Starts a command spec builder for the given command name.
     *
     * @param name primary command name
     * @return command spec builder
     */
    protected static CommandSpec.Builder command(String name)
    {
        return CommandSpec.builder(name);
    }

    @Override
    public final CommandSpec commandSpec()
    {
        return commandSpec;
    }

    @Override
    public final void bindApi(PlexApi api)
    {
        this.api = api;
    }

    @Override
    public final void bindModule(PlexModule module)
    {
        this.module = module;
    }

    @Override
    public final LiteralCommandNode<CommandSourceStack> buildCommand()
    {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(getName())
                .requires(this::canUse);
        configureCommand(command);
        return command.build();
    }

    /**
     * Configures the Brigadier command tree for this command.
     *
     * <p>The default tree accepts optional greedy string arguments and dispatches
     * them to {@link #execute(CommandSender, Player, String[])}.</p>
     *
     * @param command root command literal builder
     */
    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> dispatchCommand(context, new String[0]));
        command.then(Commands.argument("args", StringArgumentType.greedyString())
                .suggests(this::suggest)
                .executes(context -> dispatchCommand(context, splitExecutionArgs(StringArgumentType.getString(context, "args")))));
    }

    /**
     * Executes this command.
     *
     * @param sender command sender
     * @param player player sender, or {@code null} when the sender is not a player
     * @param args command arguments
     * @return component to send to the sender, or {@code null} to send no response
     */
    protected abstract Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args);

    /**
     * Returns tab-completion suggestions for this command.
     *
     * @param sender command sender
     * @param alias command alias used by the sender
     * @param args current command arguments
     * @return suggested completions
     * @throws IllegalArgumentException when suggestions cannot be produced for the supplied arguments
     */
    protected @NotNull List<String> suggestions(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return List.of();
    }

    /**
     * Returns the bound Plex API.
     *
     * @return bound Plex API
     * @throws IllegalStateException when the command has not been bound to the API
     */
    protected PlexApi api()
    {
        if (api == null)
        {
            throw new IllegalStateException("Command " + getName() + " has not been bound to the Plex API");
        }
        return api;
    }

    /**
     * Sends an ampersand-colorized legacy message to an audience.
     *
     * @param audience message recipient
     * @param message legacy message text
     */
    protected void send(Audience audience, String message)
    {
        audience.sendMessage(componentFromString(message));
    }

    /**
     * Sends a component to an audience.
     *
     * @param audience message recipient
     * @param component component to send
     */
    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    /**
     * Broadcasts a MiniMessage-formatted message.
     *
     * @param miniMessage MiniMessage-formatted message
     */
    protected void broadcast(String miniMessage)
    {
        api().messages().broadcast(miniMessage);
    }

    /**
     * Broadcasts a component.
     *
     * @param component component to broadcast
     */
    protected void broadcast(Component component)
    {
        api().messages().broadcast(component);
    }

    /**
     * Checks whether a sender has a permission node.
     *
     * @param sender command sender
     * @param permission permission node to check
     * @return {@code true} when the sender can use the permission
     * @throws CommandFailException when the sender lacks the permission
     */
    protected boolean checkPermission(CommandSender sender, String permission)
    {
        if (permission.isEmpty() || isConsole(sender) || sender.hasPermission(permission))
        {
            return true;
        }
        throw new CommandFailException(messageString("noPermissionNode", permission));
    }

    /**
     * Checks whether a sender has a permission node without throwing an exception.
     *
     * @param sender command sender
     * @param permission permission node to check
     * @return {@code true} when the sender can use the permission
     */
    protected boolean silentCheckPermission(CommandSender sender, String permission)
    {
        return permission.isEmpty() || isConsole(sender) || sender.hasPermission(permission);
    }

    /**
     * Returns the standard no-permission message for this command's permission node.
     *
     * @return no-permission component
     */
    protected Component permissionMessage()
    {
        return permissionMessage(getPermission());
    }

    /**
     * Returns the standard no-permission message for a permission node.
     *
     * @param permission permission node
     * @return no-permission component
     */
    protected Component permissionMessage(String permission)
    {
        return messageComponent("noPermissionNode", permission);
    }

    /**
     * Returns a sender's UUID when the sender is a player.
     *
     * @param sender command sender
     * @return player UUID, or {@code null} for non-player senders
     */
    protected @Nullable UUID getUUID(CommandSender sender)
    {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    /**
     * Returns whether a sender is not a player.
     *
     * @param sender command sender
     * @return {@code true} when the sender is not a player
     */
    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    /**
     * Resolves a configured message as a component.
     *
     * @param key message key
     * @param objects replacement values
     * @return resolved message component
     */
    protected Component messageComponent(String key, Object... objects)
    {
        if (module != null)
        {
            return module.messageComponent(key, objects);
        }
        return api().messages().messageComponent(key, objects);
    }

    /**
     * Resolves a configured message as a component using component replacements.
     *
     * @param key message key
     * @param objects replacement components
     * @return resolved message component
     */
    protected Component messageComponent(String key, Component... objects)
    {
        if (module != null)
        {
            return module.messageComponent(key, objects);
        }
        return api().messages().messageComponent(key, objects);
    }

    /**
     * Resolves a configured message as plain text.
     *
     * @param key message key
     * @param objects replacement values
     * @return resolved message text
     */
    protected String messageString(String key, Object... objects)
    {
        if (module != null)
        {
            return module.messageString(key, objects);
        }
        return api().messages().messageString(key, objects);
    }

    /**
     * Returns this command's formatted usage component.
     *
     * @return formatted usage component
     */
    protected Component usage()
    {
        return usage(getUsage());
    }

    /**
     * Formats command usage text with the standard usage prefix.
     *
     * @param usage usage text
     * @return formatted usage component
     */
    protected Component usage(String usage)
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(usage).color(NamedTextColor.GRAY));
    }

    /**
     * Returns an online player by UUID string or name.
     *
     * @param name UUID string or player name
     * @return matching online player
     * @throws PlayerNotFoundException when no matching online player exists
     */
    protected Player getNonNullPlayer(String name)
    {
        try
        {
            UUID uuid = UUID.fromString(name);
            Player player = Bukkit.getPlayer(uuid);
            if (player != null)
            {
                return player;
            }
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

    /**
     * Returns the names of currently online players.
     *
     * @return online player names
     */
    protected List<String> onlinePlayerNames()
    {
        return api().players().onlineNames();
    }

    /**
     * Converts ampersand-colorized legacy text to a gray-default component.
     *
     * @param value legacy text
     * @return deserialized component
     */
    protected Component componentFromString(String value)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value).colorIfAbsent(NamedTextColor.GRAY);
    }

    /**
     * Converts ampersand-colorized legacy text to a component without adding a default color.
     *
     * @param value legacy text
     * @return deserialized component
     */
    protected Component noColorComponentFromString(String value)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value);
    }

    /**
     * Converts MiniMessage-formatted text to a component.
     *
     * @param value MiniMessage-formatted text
     * @return deserialized component
     */
    protected Component mmString(String value)
    {
        return api().messages().miniMessage(value);
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
            Component component = execute(sender, sender instanceof Player player ? player : null, args);
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

    private boolean canUse(CommandSourceStack source)
    {
        CommandSender sender = source.getSender();
        if (getRequiredSource() == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            return false;
        }

        if (getRequiredSource() == RequiredCommandSource.IN_GAME && sender instanceof ConsoleCommandSender)
        {
            return false;
        }

        String permission = getPermission();
        return permission.isEmpty() || sender.hasPermission(permission);
    }

    private boolean validateSourceAndPermission(CommandSender sender)
    {
        if (getRequiredSource() == RequiredCommandSource.CONSOLE && sender instanceof Player)
        {
            send(sender, messageComponent("noPermissionInGame"));
            return false;
        }

        if (getRequiredSource() == RequiredCommandSource.IN_GAME && sender instanceof ConsoleCommandSender)
        {
            send(sender, messageComponent("noPermissionConsole"));
            return false;
        }

        String permission = getPermission();
        if (!permission.isEmpty() && !sender.hasPermission(permission))
        {
            send(sender, messageComponent("noPermissionNode", permission));
            return false;
        }
        return true;
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder)
    {
        CommandSender sender = context.getSource().getSender();
        if (!canUse(context.getSource()))
        {
            return builder.buildFuture();
        }

        String remaining = builder.getRemaining();
        String[] args = splitSuggestionArgs(remaining);
        List<String> completions = suggestions(sender, aliasFromInput(context.getInput()), args);
        return suggestLastToken(builder, completions);
    }

    private CompletableFuture<Suggestions> suggestLastToken(SuggestionsBuilder builder, Collection<String> suggestions)
    {
        String remaining = builder.getRemaining();
        int tokenStart = remaining.lastIndexOf(' ') + 1;
        String currentToken = remaining.substring(tokenStart).toLowerCase(Locale.ROOT);
        SuggestionsBuilder tokenBuilder = tokenStart == 0 ? builder : builder.createOffset(builder.getStart() + tokenStart);
        for (String suggestion : suggestions)
        {
            if (suggestion.toLowerCase(Locale.ROOT).startsWith(currentToken))
            {
                tokenBuilder.suggest(suggestion);
            }
        }
        return tokenBuilder.buildFuture();
    }

    private String aliasFromInput(String input)
    {
        String trimmed = input.trim();
        if (trimmed.isEmpty())
        {
            return getName();
        }

        String label = trimmed.split("\\s+", 2)[0];
        return label.startsWith("/") ? label.substring(1) : label;
    }

    private String[] splitExecutionArgs(String rawArgs)
    {
        if (rawArgs.isBlank())
        {
            return new String[0];
        }
        return rawArgs.trim().split("\\s+");
    }

    private String[] splitSuggestionArgs(String rawArgs)
    {
        if (rawArgs.isEmpty())
        {
            return new String[] {""};
        }
        return rawArgs.stripLeading().split("\\s+", -1);
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
        return message == null ? componentFromString(ex.getClass().getSimpleName()) : mmString(message);
    }
}
