package dev.plex.util.minimessage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;

public class SafeMiniMessage
{
    // Opt in to visual tags so future standard tags do not silently expand player permissions.
    public static final MiniMessage MINI_MESSAGE = MiniMessage.builder().tags(TagResolver.resolver(
            StandardTags.color(),
            StandardTags.shadowColor(),
            StandardTags.font(),
            StandardTags.decorations(TextDecoration.BOLD),
            StandardTags.decorations(TextDecoration.ITALIC),
            StandardTags.decorations(TextDecoration.UNDERLINED),
            StandardTags.decorations(TextDecoration.STRIKETHROUGH),
            StandardTags.reset(),
            StandardTags.gradient(),
            StandardTags.rainbow(),
            StandardTags.transition(),
            StandardTags.pride(),
            StandardTags.sprite(),
            StandardTags.sequentialHead())).build();

    public static Component mmDeserialize(String text, TagResolver... placeholders)
    {
        return MINI_MESSAGE.deserialize(text, placeholders);
    }

    public static String mmSerialize(Component component)
    {
        return MiniMessage.miniMessage().serialize(component);
    }
}
