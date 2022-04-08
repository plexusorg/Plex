package dev.plex.command.blocking;

import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;

@Data
public class BlockedCommand
{
    private String message;
    private String requiredLevel;
    private String regex;
    private String command;
    private List<String> commandAliases = Lists.newArrayList();
}
