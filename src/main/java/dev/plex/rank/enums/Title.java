package dev.plex.rank.enums;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.json.JSONObject;

@Getter
public enum Title
{
    MASTER_BUILDER(0, ChatColor.AQUA + "a " + ChatColor.DARK_AQUA + "Master Builder", "Master Builder", "&8[&3Master Builder&8]"),
    DEV(1, ChatColor.AQUA + "a " + ChatColor.DARK_PURPLE + "Developer", "Developer", "&8[&5Developer&8]"),
    OWNER(2, ChatColor.AQUA + "an " + ChatColor.BLUE + "Owner", "Owner", "&8[&9Owner&8]");

    private final int level;

    @Setter
    private String loginMessage;

    @Setter
    private String readable;

    @Setter
    private String prefix;

    Title(int level, String loginMessage, String readable, String prefix)
    {
        this.level = level;
        this.loginMessage = loginMessage;
        this.readable = readable;
        this.prefix = prefix;
    }

    public String getPrefix()
    {
        return MiniMessage.miniMessage().serialize(LegacyComponentSerializer.legacyAmpersand().deserialize(this.prefix));
    }

    public JSONObject toJSON()
    {
        JSONObject object = new JSONObject();
        object.put("prefix", this.prefix);
        object.put("loginMessage", this.loginMessage);
        return new JSONObject().put(this.name(), object);
    }
}
