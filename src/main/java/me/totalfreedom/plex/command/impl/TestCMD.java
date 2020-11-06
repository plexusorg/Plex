package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "tst,tast", description = "HELLO")
public class TestCMD extends PlexCommand
{
    public TestCMD()
    {
        super("test");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        send(tl("variableTest", sender.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        if (args.length == 1)
        {
            return Arrays.asList("WHATTHEFAWK", "LUL");
        }
        return ImmutableList.of();
    }
}
