package dev.plex.util;

import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.format.NamedTextColor;

public class RandomUtil
{

    public static NamedTextColor getRandomColor()
    {
        NamedTextColor[] colors = NamedTextColor.NAMES.values().toArray(NamedTextColor[]::new);
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
