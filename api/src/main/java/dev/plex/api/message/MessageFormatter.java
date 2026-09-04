package dev.plex.api.message;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Resolves named placeholders in configured messages.
 */
public final class MessageFormatter
{
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-z][a-z0-9_]*)}");
    private MessageFormatter()
    {
    }

    /** Resolves scalar placeholders without parsing the resulting message. */
    public static String formatString(String message, MessagePlaceholder... placeholders)
    {
        Map<String, String> values = new HashMap<>();
        for (MessagePlaceholder placeholder : placeholders)
        {
            if (placeholder.component() != null)
            {
                throw new IllegalArgumentException("Component placeholder cannot be used in a string message: " + placeholder.name());
            }
            values.put(placeholder.name(), placeholder.value());
        }
        return replaceStrings(message, values);
    }

    /** Resolves scalar and component placeholders and parses the resulting MiniMessage. */
    public static Component formatComponent(String message, MessagePlaceholder... placeholders)
    {
        return formatComponent(message, MiniMessage.miniMessage()::deserialize, placeholders);
    }

    /**
     * Resolves placeholders and parses the message with the supplied component renderer.
     */
    public static Component formatComponent(String message, Function<String, Component> renderer,
                                            MessagePlaceholder... placeholders)
    {
        Map<String, String> values = new HashMap<>();
        for (MessagePlaceholder placeholder : placeholders)
        {
            if (placeholder.value() != null)
            {
                values.put(placeholder.name(), placeholder.value());
            }
        }
        Component component = renderer.apply(replaceStrings(message, values));
        for (MessagePlaceholder placeholder : placeholders)
        {
            if (placeholder.component() != null)
            {
                component = component.replaceText(builder -> builder.matchLiteral(token(placeholder))
                        .replacement(placeholder.component()).build());
            }
        }
        return component;
    }

    private static String token(MessagePlaceholder placeholder)
    {
        return "{" + placeholder.name() + "}";
    }

    private static String replaceStrings(String message, Map<String, String> values)
    {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(message);
        StringBuilder result = new StringBuilder();
        while (matcher.find())
        {
            String replacement = values.get(matcher.group(1));
            matcher.appendReplacement(result, replacement == null ? matcher.group() : Matcher.quoteReplacement(replacement));
        }
        return matcher.appendTail(result).toString();
    }
}
