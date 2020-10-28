package me.totalfreedom.plex.rank;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexLog;
import org.json.JSONObject;

public class RankManager
{
    private final File defaultRanks;

    public RankManager()
    {
        defaultRanks = new File(new File(Plex.get().getDataFolder() + File.separator + "ranks"), "default-ranks.json");
    }

    public void generateDefaultRanks()
    {
        if (defaultRanks.exists())
        {
            return;
        }
        else
        {
            try
            {
                defaultRanks.createNewFile();

                List<DefaultRankObj> ranks = Lists.newArrayList();
                for (Rank rank : Rank.values())
                {
                    ranks.add(new DefaultRankObj(rank));
                }

                JSONObject obj = new JSONObject();
                if (obj.length() == 0)
                {
                    obj.put("ranks", ranks);

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
    }
}
