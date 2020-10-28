package me.totalfreedom.plex.rank.enums;

import org.bukkit.ChatColor;

public enum Rank
{
    IMPOSTER(-1, ChatColor.AQUA + "an " + ChatColor.YELLOW + "Imposter", ChatColor.YELLOW + "[Imp]"),
    NONOP(0, ChatColor.WHITE + "a " + ChatColor.WHITE + "Non-Op", ChatColor.WHITE + ""),
    OP(1, ChatColor.GREEN + "an " + ChatColor.GREEN + "Operator", ChatColor.GREEN + "[OP]"),
    ADMIN(2, ChatColor.DARK_GREEN + "an " + ChatColor.DARK_GREEN + "Admin", ChatColor.DARK_GREEN + "[Admin]"),
    SENIOR_ADMIN(3, ChatColor.GOLD + "a " + ChatColor.GOLD + "Senior Admin", ChatColor.GOLD + "[SrA]"),
    EXECUTIVE(4, ChatColor.RED + "an " + ChatColor.RED + "Executive", ChatColor.RED + "[Exec]");

    private final String loginMessage;
    private final String prefix;

    Rank(int level, String loginMessage, String prefix)
    {
        this.loginMessage = loginMessage;
        this.prefix = prefix;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public String getLoginMSG()
    {
        return loginMessage;
    }
}
