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

    protected SimplePlexCommand(CommandSpec commandSpec)
    {
        this.commandSpec = commandSpec;
    }

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

    protected void configureCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> dispatchCommand(context, new String[0]));
        command.then(Commands.argument("args", StringArgumentType.greedyString())
                .suggests(this::suggest)
                .executes(context -> dispatchCommand(context, splitExecutionArgs(StringArgumentType.getString(context, "args")))));
    }

    protected abstract Component execute(@NotNull CommandSender sender, @Nullable Player player, @NotNull String[] args);

    protected @NotNull List<String> suggestions(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return List.of();
    }

    protected PlexApi api()
    {
        if (api == null)
        {
            throw new IllegalStateException("Command " + getName() + " has not been bound to the Plex API");
        }
        return api;
    }

    protected void send(Audience audience, String message)
    {
        audience.sendMessage(componentFromString(message));
    }

    protected void send(Audience audience, Component component)
    {
        audience.sendMessage(component);
    }

    protected void broadcast(String miniMessage)
    {
        api().messages().broadcast(miniMessage);
    }

    protected void broadcast(Component component)
    {
        api().messages().broadcast(component);
    }

    protected boolean checkPermission(CommandSender sender, String permission)
    {
        if (permission.isEmpty() || isConsole(sender) || sender.hasPermission(permission))
        {
            return true;
        }
        throw new CommandFailException(messageString("noPermissionNode", permission));
    }

    protected boolean silentCheckPermission(CommandSender sender, String permission)
    {
        return permission.isEmpty() || isConsole(sender) || sender.hasPermission(permission);
    }

    protected Component permissionMessage()
    {
        return permissionMessage(getPermission());
    }

    protected Component permissionMessage(String permission)
    {
        return messageComponent("noPermissionNode", permission);
    }

    protected @Nullable UUID getUUID(CommandSender sender)
    {
        return sender instanceof Player player ? player.getUniqueId() : null;
    }

    protected boolean isConsole(CommandSender sender)
    {
        return !(sender instanceof Player);
    }

    protected Component messageComponent(String key, Object... objects)
    {
        if (module != null)
        {
            return module.messageComponent(key, objects);
        }
        return api().messages().messageComponent(key, objects);
    }

    protected Component messageComponent(String key, Component... objects)
    {
        if (module != null)
        {
            return module.messageComponent(key, objects);
        }
        return api().messages().messageComponent(key, objects);
    }

    protected String messageString(String key, Object... objects)
    {
        if (module != null)
        {
            return module.messageString(key, objects);
        }
        return api().messages().messageString(key, objects);
    }

    protected Component usage()
    {
        return usage(getUsage());
    }

    protected Component usage(String usage)
    {
        return messageComponent("correctUsagePrefix").append(componentFromString(usage).color(NamedTextColor.GRAY));
    }

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

    protected List<String> onlinePlayerNames()
    {
        return api().players().onlineNames();
    }

    protected Component componentFromString(String value)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value).colorIfAbsent(NamedTextColor.GRAY);
    }

    protected Component noColorComponentFromString(String value)
    {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(value);
    }

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
