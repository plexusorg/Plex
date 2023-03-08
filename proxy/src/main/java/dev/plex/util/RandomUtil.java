package dev.plex.util;

import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.ThreadLocalRandom;

public class RandomUtil
{
    public static NamedTextColor getRandomColor()
    {
        NamedTextColor[] colors = NamedTextColor.NAMES.values().stream().filter(namedTextColor -> namedTextColor != NamedTextColor.BLACK && namedTextColor != NamedTextColor.DARK_BLUE).toArray(NamedTextColor[]::new);
        return colors[randomNum(colors.length)];
    }

    public static boolean randomBoolean()
    {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static int randomNum()
    {
        return ThreadLocalRandom.current().nextInt();
    }

    public static int randomNum(int limit)
    {
        return ThreadLocalRandom.current().nextInt(limit);
    }

    public static int randomNum(int start, int limit)
    {
        return ThreadLocalRandom.current().nextInt(start, limit);
    }
}
