package dev.plex.rank;

import com.google.common.collect.Maps;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexLog;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.json.JSONTokener;

public class RankManager
{
    private final File defaultRanks;

    public RankManager()
    {
        File ranksFolder = new File(Plex.get().getDataFolder() + File.separator + "ranks");
        if (!ranksFolder.exists())
        {
            ranksFolder.mkdir();
        }

        defaultRanks = new File(ranksFolder, "default-ranks.json");
    }

    public void generateDefaultRanks()
    {
        if (defaultRanks.exists())
        {
            return;
        }
        try
        {
            defaultRanks.createNewFile();

            Map<String, DefaultRankObj> rankMap = Maps.newHashMap();
            for (Rank rank : Rank.values())
            {
                rankMap.put(rank.name().toUpperCase(), new DefaultRankObj(rank));
            }

            JSONObject obj = new JSONObject();
            if (obj.length() == 0)
            {
                obj.put("ranks", rankMap);

                FileWriter writer = new FileWriter(defaultRanks);
                writer.append(obj.toString(4));
                writer.flush();
                writer.close();
                PlexLog.log("Generating default-ranks.json");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void importDefaultRanks()
    {
        if (!defaultRanks.exists())
        {
            return;
        }

        try
        {
            FileInputStream stream = new FileInputStream(defaultRanks);
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject object = new JSONObject(tokener);
            JSONObject rankObj = object.getJSONObject("ranks");
            for (Rank rank : Rank.values())
            {
                if (rankObj.isNull(rank.name().toUpperCase()))
                {
                    continue;
                }
                rank.setLoginMessage(rankObj.getJSONObject(rank.name().toUpperCase()).getString("loginMSG"));
                rank.setPrefix(rankObj.getJSONObject(rank.name().toUpperCase()).getString("prefix")); //should i even be doing this
                rank.setPermissions(rankObj.getJSONObject(rank.name().toUpperCase()).getJSONArray("permissions").toList().stream().map(Object::toString).collect(Collectors.toList()));

            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public boolean isAdmin(PlexPlayer plexPlayer)
    {
        return !plexPlayer.getRank().isEmpty() && plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN);
    }

    public boolean isSeniorAdmin(PlexPlayer plexPlayer)
    {
        return !plexPlayer.getRank().isEmpty() && plexPlayer.getRankFromString().isAtLeast(Rank.SENIOR_ADMIN);
    }
}
