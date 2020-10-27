package me.totalfreedom.plex.rank;

import com.google.common.collect.Lists;
import lombok.Getter;
import me.totalfreedom.plex.rank.enums.Rank;

import java.util.List;

@Getter
public class DefaultRankObj
{

    private String name;
    private List<String> permissions;

    public DefaultRankObj(Rank rank)
    {
        this.name = rank.name().toUpperCase();
        this.permissions = Lists.newArrayList();
    }


}
