package dev.plex.rank.enums;

import dev.plex.util.PlexUtils;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.json.JSONObject;

@Getter
public enum Rank
{
    IMPOSTOR(-1, ChatColor.AQUA + "an " + ChatColor.YELLOW + "Impostor", "Impostor", "&8[&eImp&8]"),
    NONOP(0, "a " + ChatColor.WHITE + "Non-Op", "Non-Op", ""),
    OP(1, "an " + ChatColor.GREEN + "Operator", "Operator", "&8[&aOP&8]"),
    ADMIN(2, "an " + ChatColor.DARK_GREEN + "Admin", "Admin", "&8[&2Admin&8]"),
    SENIOR_ADMIN(3, "a " + ChatColor.GOLD + "Senior Admin", "Senior Admin", "&8[&6SrA&8]"),
    EXECUTIVE(4, "an " + ChatColor.RED + "Executive", "Executive", "&8[&cExec&8]");

    private final int level;

    @Setter
    private String loginMessage;

    @Setter
    private String readable;

    @Setter
    private String prefix;

    Rank(int level, String loginMessage, String readable, String prefix)
    {
        this.level = level;
        this.loginMessage = loginMessage;
        this.readable = readable;
        this.prefix = prefix;
    }

    public boolean isAtLeast(Rank rank)
    {
        return this.level >= rank.getLevel();
    }

    public String getPrefix()
    {
        return PlexUtils.colorize(this.prefix);
    }

    public JSONObject toJSON()
    {
        JSONObject object = new JSONObject();
        object.put("prefix", this.prefix);
        object.put("loginMessage", this.loginMessage);
        return new JSONObject().put(this.name(), object);
    }
}
