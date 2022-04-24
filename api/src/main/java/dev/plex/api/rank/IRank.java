package dev.plex.api.rank;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.json.JSONObject;

public interface IRank
{

    int getLevel();

    String getLoginMessage();

    void setLoginMessage(String message);

    String getReadable();

    Component getPrefix();

    void setPrefix(String prefix);

    NamedTextColor getColor();

    boolean isAtLeast(IRank rank);

    JSONObject toJSON();
}