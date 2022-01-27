package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;

import java.util.Arrays;
import java.util.List;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "tst,tast", description = "HELLO")
public class TestCMD extends PlexCommand
{
    public TestCMD()
    {
        super("test");
    }

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        send(tl("variableTest", sender.getName()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        if (args.length == 1)
        {
            return Arrays.asList("WHATTHEFAWK", "LUL");
        }
        return ImmutableList.of();
    }
}
