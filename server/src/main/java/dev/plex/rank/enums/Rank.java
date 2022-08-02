package dev.plex.rank.enums;

import dev.plex.util.PlexUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.json.JSONObject;

@Getter
public enum Rank
{
    IMPOSTOR(-1, "<aqua>an <yellow>Impostor<reset>", "Impostor", "<dark_gray>[<yellow>Imp<dark_gray>]", NamedTextColor.YELLOW),
    NONOP(0, "a <white>Non-Op<reset>", "Non-Op", "", NamedTextColor.WHITE),
    OP(1, "an <green>Op<reset>", "Operator", "<dark_gray>[<green>OP<dark_gray>]", NamedTextColor.GREEN),
    ADMIN(2, "an <dark_green>Admin<reset>", "Admin", "<dark_gray>[<green>Admin<dark_gray>]", NamedTextColor.DARK_GREEN),
    SENIOR_ADMIN(3, "a <gold>Senior Admin<reset>", "Senior Admin", "<dark_gray>[<gold>SrA<dark_gray>]", NamedTextColor.GOLD),
    EXECUTIVE(4, "an <red>Executive<reset>", "Executive", "<dark_gray>[<red>Exec<dark_gray>]", NamedTextColor.RED);

    private final int level;
    @Getter
    private final NamedTextColor color;
    @Setter
    private String loginMessage;
    @Setter
    private String readable;
    @Setter
    private String prefix;

    Rank(int level, String loginMessage, String readable, String prefix, NamedTextColor color)
    {
        this.level = level;
        this.loginMessage = loginMessage;
        this.readable = readable;
        this.prefix = prefix;
        this.color = color;
    }

    public boolean isAtLeast(Rank rank)
    {
        return this.level >= rank.getLevel();
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
