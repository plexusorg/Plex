package dev.plex.rank.enums;

import com.google.common.collect.Lists;

import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.json.JSONObject;

@Getter
public enum Rank
{
    IMPOSTOR(-1, ChatColor.AQUA + "an " + ChatColor.YELLOW + "Impostor", "Impostor", "&8[&eImp&8]"),
    NONOP(0, "a " + ChatColor.WHITE + "Non-Op", "Non-Op", ""),
    OP(1, "an " + ChatColor.GREEN + "Operator", "Operator", "&8[&aOp&8]"),
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

    public JSONObject toJSON()
    {
        JSONObject object = new JSONObject();
        object.put("prefix", this.prefix);
        object.put("loginMessage", this.loginMessage);
        return new JSONObject().put(this.name(), object);
    }
}
