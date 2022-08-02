package dev.plex.rank.enums;

import dev.plex.util.PlexUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.json.JSONObject;

@Getter
public enum Title
{
    MASTER_BUILDER(0, "<aqua>a <dark_aqua>Master Builder<reset>", "Master Builder", "<dark_gray>[<dark_aqua>Master Builder<dark_gray>]", NamedTextColor.DARK_AQUA),
    DEV(1, "<aqua>a <dark_purple>Developer<reset>", "Developer", "<dark_gray>[<dark_purple>Developer<dark_gray>]", NamedTextColor.DARK_PURPLE),
    OWNER(2, "<aqua>an <blue>Owner<reset>", "Owner", "<dark_gray>[<blue>Owner<dark_gray>]", NamedTextColor.BLUE);

    private final int level;
    @Getter
    private final NamedTextColor color;
    @Setter
    private String loginMessage;
    @Setter
    private String readable;
    @Setter
    private String prefix;

    Title(int level, String loginMessage, String readable, String prefix, NamedTextColor color)
    {
        this.level = level;
        this.loginMessage = loginMessage;
        this.readable = readable;
        this.prefix = prefix;
        this.color = color;
    }

    public Component getPrefix()
    {
        return PlexUtils.mmDeserialize(this.prefix);
    }

    public JSONObject toJSON()
    {
        JSONObject object = new JSONObject();
        object.put("prefix", this.prefix);
        object.put("loginMessage", this.loginMessage);
        return new JSONObject().put(this.name(), object);
    }
}
