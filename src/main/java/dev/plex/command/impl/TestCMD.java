package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import java.util.Arrays;
import java.util.List;
import dev.plex.command.PlexCommand;
import dev.plex.rank.enums.Rank;

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
