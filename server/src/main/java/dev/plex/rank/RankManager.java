package dev.plex.rank;

import dev.plex.Plex;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.rank.enums.Title;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RankManager
{
    private final File options;

    public RankManager()
    {
        File ranksFolder = new File(Plex.get().getDataFolder() + File.separator + "ranks");
        if (!ranksFolder.exists())
        {
            ranksFolder.mkdir();
        }

        options = new File(ranksFolder, "options.json");
    }

    @SneakyThrows
    public void generateDefaultRanks()
    {
        if (options.exists())
        {
            return;
        }
        JSONObject object = new JSONObject();
        object.put("ranks", new JSONArray().putAll(Arrays.stream(Rank.values()).map(Rank::toJSON).collect(Collectors.toList())));
        object.put("titles", new JSONArray().putAll(Arrays.stream(Title.values()).map(Title::toJSON).collect(Collectors.toList())));
        FileWriter writer = new FileWriter(options);
        writer.append(object.toString(4));
        writer.flush();
        writer.close();
    }

    public Rank getRankFromString(String rank)
    {
        return Rank.valueOf(rank.toUpperCase());
    }

    public void importDefaultRanks()
    {
        if (!options.exists())
        {
            return;
        }

        try (FileInputStream fis = new FileInputStream(options))
        {
            JSONTokener tokener = new JSONTokener(fis);
            JSONObject object = new JSONObject(tokener);

            JSONArray ranks = object.getJSONArray("ranks");
            ranks.forEach(r ->
            {
                JSONObject rank = new JSONObject(r.toString());
                String key = rank.keys().next();
                Rank.valueOf(key).setLoginMessage(rank.getJSONObject(key).getString("loginMessage"));
                Rank.valueOf(key).setPrefix(rank.getJSONObject(key).getString("prefix"));
            });

            JSONArray titles = object.getJSONArray("titles");
            titles.forEach(t ->
            {
                JSONObject title = new JSONObject(t.toString());
                String key = title.keys().next();
                Title.valueOf(key).setLoginMessage(title.getJSONObject(key).getString("loginMessage"));
                Title.valueOf(key).setPrefix(title.getJSONObject(key).getString("prefix"));
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public Component getConfigPrefixes(PlexPlayer plexPlayer)
    {
        if (Plex.get().config.contains("titles.owners") && Plex.get().config.getStringList("titles.owners").contains(plexPlayer.getName()))
        {
            return Title.OWNER.getPrefix();
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return Title.DEV.getPrefix();
        }
        if (Plex.get().config.contains("titles.masterbuilders") && Plex.get().config.getStringList("titles.masterbuilders").contains(plexPlayer.getName()))
        {
            return Title.MASTER_BUILDER.getPrefix();
        }
        if (Plex.get().getSystem().equalsIgnoreCase("ranks") && isAdmin(plexPlayer))
        {
            return plexPlayer.getRankFromString().getPrefix();
        }
        return null;
    }

    public Component getPrefix(PlexPlayer plexPlayer)
    {
        if (plexPlayer.getPrefix() != null && !plexPlayer.getPrefix().isEmpty())
        {
            return SafeMiniMessage.mmDeserializeWithoutEvents(plexPlayer.getPrefix());
        }
        if (getConfigPrefixes(plexPlayer) != null)
        {
            return getConfigPrefixes(plexPlayer);
        }
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault") && Plex.get().getSystem().equalsIgnoreCase("permissions"))
        {
            return VaultHook.getPrefix(plexPlayer);
        }
        return null;
    }

    public String getLoginMessage(PlexPlayer plexPlayer)
    {
        String prepend;
        // We don't want to prepend the "<player> is" if the login message is custom
        if (!plexPlayer.getLoginMessage().isEmpty())
        {
            return plexPlayer.getLoginMessage()
                    .replace("%player%", plexPlayer.getName())
                    .replace("%rank%", plexPlayer.getRank());
        }
        else
        {
            prepend = MiniMessage.miniMessage().serialize(Component.text(plexPlayer.getName() + " is ").color(NamedTextColor.AQUA));
        }
        if (Plex.get().config.contains("titles.owners") && Plex.get().config.getStringList("titles.owners").contains(plexPlayer.getName()))
        {
            return prepend + Title.OWNER.getLoginMessage();
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return prepend + Title.DEV.getLoginMessage();
        }
        if (Plex.get().config.contains("titles.masterbuilders") && Plex.get().config.getStringList("titles.masterbuilders").contains(plexPlayer.getName()))
        {
            return prepend + Title.MASTER_BUILDER.getLoginMessage();
        }
        if (Plex.get().getSystem().equalsIgnoreCase("ranks") && isAdmin(plexPlayer))
        {
            return prepend + plexPlayer.getRankFromString().getLoginMessage();
        }
        return "";
    }

    public NamedTextColor getColor(PlexPlayer plexPlayer)
    {
        if (Plex.get().config.contains("titles.owners") && Plex.get().config.getStringList("titles.owners").contains(plexPlayer.getName()))
        {
            return Title.OWNER.getColor();
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return Title.DEV.getColor();
        }
        if (Plex.get().config.contains("titles.masterbuilders") && Plex.get().config.getStringList("titles.masterbuilders").contains(plexPlayer.getName()))
        {
            return Title.MASTER_BUILDER.getColor();
        }
        if (Plex.get().getSystem().equalsIgnoreCase("ranks") && isAdmin(plexPlayer))
        {
            return plexPlayer.getRankFromString().getColor();
        }
        return NamedTextColor.WHITE;
    }

    public boolean isAdmin(PlexPlayer plexPlayer)
    {
        return !plexPlayer.getRank().isEmpty() && plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN) && plexPlayer.isAdminActive();
    }

    public boolean isSeniorAdmin(PlexPlayer plexPlayer)
    {
        return !plexPlayer.getRank().isEmpty() && plexPlayer.getRankFromString().isAtLeast(Rank.SENIOR_ADMIN) && plexPlayer.isAdminActive();
    }
}
