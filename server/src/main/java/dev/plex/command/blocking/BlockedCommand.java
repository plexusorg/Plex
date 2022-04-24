package dev.plex.command.blocking;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Data;
import net.kyori.adventure.text.Component;

@Data
public class BlockedCommand
{
    private Component message;
    private String requiredLevel;
    private String regex;
    private String command;
    private List<String> commandAliases = Lists.newArrayList();
}
