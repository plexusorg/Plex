package dev.plex.rank.enums;

import org.bukkit.ChatColor;

public enum Title
{
    MASTER_BUILDER(0, ChatColor.AQUA + "a " + ChatColor.DARK_AQUA + "Master Builder", "Master Builder", ChatColor.DARK_AQUA + "[MB]"),
    DEV(1, ChatColor.AQUA + "a " + ChatColor.DARK_PURPLE + "Developer", "Developer", ChatColor.DARK_PURPLE + "[DEV]"),
    OWNER(2, ChatColor.AQUA + "an " + ChatColor.BLUE + "Owner", "Owner", ChatColor.BLUE + "[Owner]");

    private int level;
    private String loginMSG;
    private String readable;
    private String prefix;

    Title(int level, String loginMSG, String readable, String prefix)
    {
        this.level = level;
        this.loginMSG = loginMSG;
        this.readable = readable;
        this.prefix = prefix;
    }
}
