package me.totalfreedom.plex.message;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.util.PlexLog;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

public class MessageManager
{
    private final File messages;

    public MessageManager()
    {
        this.messages = new File(Plex.get().getDataFolder(), "messages.json");
    }

    public void generateMessages()
    {
        if (messages.exists())
        {
            return;
        }
        try
        {
            messages.createNewFile();

            JSONObject obj = new JSONObject();
            if (obj.length() == 0)
            {
                obj.put("test", "this is a test message!");
                obj.put("noAdminWorldBlockPlace", "&cYou are not allowed to place blocks in the admin world!");
                obj.put("noAdminWorldBlockBreak", "&cYou are not allowed to break blocks in the admin world!");
                FileWriter writer = new FileWriter(messages);
                writer.append(obj.toString(4));
                writer.flush();
                writer.close();
                PlexLog.log("Generating messages.json");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public String getMessage(String s)
    {
        if (!messages.exists())
            return null;

        try
        {
            FileInputStream stream = new FileInputStream(messages);
            JSONTokener tokener = new JSONTokener(stream);
            JSONObject object = new JSONObject(tokener);
            return (String) object.get(s);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }
}