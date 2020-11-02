package me.totalfreedom.plex.rank.enums;

import com.google.common.collect.Lists;
import org.bukkit.ChatColor;

import java.util.List;

public enum Rank
{
    IMPOSTOR(-1, ChatColor.AQUA + "an " + ChatColor.YELLOW + "Impostor", ChatColor.YELLOW + "[Imp]"),
    NONOP(0, ChatColor.WHITE + "a " + ChatColor.WHITE + "Non-Op", ChatColor.WHITE + ""),
    OP(1, ChatColor.GREEN + "an " + ChatColor.GREEN + "Operator", ChatColor.GREEN + "[OP]"),
    ADMIN(2, ChatColor.DARK_GREEN + "an " + ChatColor.DARK_GREEN + "Admin", ChatColor.DARK_GREEN + "[Admin]"),
    SENIOR_ADMIN(3, ChatColor.GOLD + "a " + ChatColor.GOLD + "Senior Admin", ChatColor.GOLD + "[SrA]"),
    EXECUTIVE(4, ChatColor.RED + "an " + ChatColor.RED + "Executive", ChatColor.RED + "[Exec]");

    private String loginMessage;
    private  String prefix;
    private int level;
    private List<String> permissions;

    Rank(int level, String loginMessage, String prefix)
    {
        this.level = level;
        this.loginMessage = loginMessage;
        this.prefix = prefix;
        this.permissions = Lists.newArrayList();
    }

    public String getPrefix()
    {
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public String getLoginMSG()
    {
        return ChatColor.translateAlternateColorCodes('&', loginMessage);
    }

    public int getLevel() {
        return level;
    }

    public void setLoginMessage(String msg)
    {
        this.loginMessage = msg;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public boolean isAtLeast(Rank rank)
    {
        return getLevel() >= rank.getLevel();
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }
}
