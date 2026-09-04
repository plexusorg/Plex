package dev.plex.util.minimessage;

import com.google.common.collect.ImmutableList;

import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.Context;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.ParsingException;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SafeMiniMessage
{
    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(new SafeMiniMessageTagResolver()).build();
    private static final TagResolver WITHOUT_EVENTS = new TagResolver()
    {
        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context context)
        {
            if (!has(name))
            {
                return null;
            }
            // Keep standard argument validation while consuming event tags without applying their styles.
            TagResolver.standard().resolve(name, arguments, context);
            return Tag.styling();
        }

        @Override
        public boolean has(@NotNull String name)
        {
            return name.equals("click") || name.equals("hover");
        }
    };

    public static Component mmDeserialize(String text)
    {
        return MINI_MESSAGE.deserialize(text);
    }

    public static Component mmDeserializeWithoutEvents(String text)
    {
        return MINI_MESSAGE.deserialize(text, WITHOUT_EVENTS);
    }

    public static String mmSerialize(Component component)
    {
        return MINI_MESSAGE.serialize(component);
    }

    public static String mmSerializeWithoutEvents(Component component)
    {
        return mmSerialize(component.clickEvent(null).hoverEvent(null));
    }

    public static class SafeMiniMessageTagResolver implements TagResolver
    {
        private static final TagResolver STANDARD_RESOLVER = TagResolver.standard();
        private static final List<String> IGNORED_TAGS = ImmutableList.of("obfuscated", "obf", "br", "newline", "lang", "key", "translate");

        @Override
        public @Nullable Tag resolve(@NotNull String name, @NotNull ArgumentQueue arguments, @NotNull Context ctx) throws ParsingException
        {
            return IGNORED_TAGS.contains(name.toLowerCase()) ? null : STANDARD_RESOLVER.resolve(name, arguments, ctx);
        }

        @Override
        public boolean has(@NotNull String name)
        {
            return STANDARD_RESOLVER.has(name);
        }
    }
}
