package me.totalfreedom.plex.rank.enums;

import org.bukkit.ChatColor;

public enum Title
{
    MASTER_BUILDER(0, ChatColor.AQUA + "a " + ChatColor.DARK_AQUA + "Master Builder", ChatColor.DARK_AQUA + "[MB]"),
    DEV(1, ChatColor.AQUA + "a " + ChatColor.DARK_PURPLE + "Developer", ChatColor.DARK_PURPLE + "[DEV]"),
    OWNER(2, ChatColor.AQUA + "an " + ChatColor.BLUE + "Owner", ChatColor.BLUE + "[Owner]");

    private int level;
    private String loginMSG;
    private String prefix;

    Title(int level, String loginMSG, String prefix)
    {
        this.level = level;
        this.loginMSG = loginMSG;
        this.prefix = prefix;
    }
}
