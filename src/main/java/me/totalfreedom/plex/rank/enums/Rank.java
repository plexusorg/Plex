package me.totalfreedom.plex.rank.enums;

import org.bukkit.ChatColor;

public enum Rank
{

    IMPOSTER(0, ChatColor.AQUA + "an " + ChatColor.YELLOW + "Imposter", ChatColor.YELLOW + "[IMP]"),
    ADMIN(1, ChatColor.AQUA + "an " + ChatColor.AQUA + "Admin", ChatColor.AQUA + "[ADMIN]"),
    SENIOR_ADMIN(2, ChatColor.AQUA + "a " + ChatColor.LIGHT_PURPLE + "Senior Admin", ChatColor.LIGHT_PURPLE + "[SrA]"),
    EXECUTIVE(3, ChatColor.AQUA + "an " + ChatColor.RED + "Executive", ChatColor.RED + "[EXEC]");

    private int level;
    private String loginMSG;
    private String prefix;

    Rank(int level, String loginMSG, String prefix)
    {
        this.level = level;
        this.loginMSG = loginMSG;
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLoginMSG() {
        return loginMSG;
    }
}
