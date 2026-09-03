package dev.plex.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

record BanReason(String text, boolean rollback)
{
    static BanReason parse(String normalizedReason)
    {
        if (normalizedReason == null)
        {
            return new BanReason(null, false);
        }
        List<String> parts = new ArrayList<>(Arrays.asList(normalizedReason.split(" ")));
        boolean rollback = parts.getFirst().equals("-rb") || parts.getLast().equals("-rb");
        if (parts.getFirst().equals("-rb")) parts.removeFirst();
        if (!parts.isEmpty() && parts.getLast().equals("-rb")) parts.removeLast();
        return new BanReason(parts.isEmpty() ? null : String.join(" ", parts), rollback);
    }
}
