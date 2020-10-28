package me.totalfreedom.plex.rank;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Getter;
import me.totalfreedom.plex.rank.enums.Rank;

@Getter
public class DefaultRankObj
{
    private final String name;
    private final List<String> permissions;

    public DefaultRankObj(Rank rank)
    {
        this.name = rank.name().toUpperCase();
        this.permissions = Lists.newArrayList();
    }
}
