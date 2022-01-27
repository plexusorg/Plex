package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.List;

@CommandParameters(description = "Get the name history of a player", usage = "/<command> <player>", aliases = "nh")
@CommandPermissions(level = Rank.OP)
public class NameHistoryCMD extends PlexCommand
{
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy 'at' HH:mm:ss");

    public NameHistoryCMD()
    {
        super("namehistory");
    }

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        String username = args[0];
        JSONArray array;
        try
        {
            JSONObject profile = (JSONObject)PlexUtils.simpleGET("https://api.mojang.com/users/profiles/minecraft/" + username);
            String uuid = (String)profile.get("id");
            array = (JSONArray)PlexUtils.simpleGET("https://api.mojang.com/user/profiles/" + uuid + "/names");
        }
        catch (ParseException | IOException ex)
        {
            send(tl("nameHistoryFail", username));
            return;
        }

        array.sort(Comparator.reverseOrder());

        StringBuilder result = new StringBuilder()
                .append(tl("nameHistoryTitle", username))
                .append("\n");
        for (Object o : array)
        {
            JSONObject object = (JSONObject)o;
            Object changedToAt = object.get("changedToAt");
            if (changedToAt == null)
            {
                changedToAt = "O";
            }
            else
            {
                changedToAt = DATE_FORMAT.format(changedToAt);
            }
            result.append(tl("nameHistoryBody", object.get("name"), changedToAt))
                    .append("\n");
        }
        send(result.toString());
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}