package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.PlexBase;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class PlexCommand extends PlexBase implements CommandExecutor, TabCompleter
{
    private static final String COMMAND_PREFIX = "Command_";
    private static CommandMap COMMAND_MAP;

    private final CommandParameters params;
    private final CommandPermissions perms;
    private final String name;
    private final String description;
    private final String usage;
    private final List<String> aliases;
    private final Rank level;
    private final RequiredCommandSource source;

    protected PlexCommand()
    {
        this.params = this.getClass().getAnnotation(CommandParameters.class);
        this.perms = this.getClass().getAnnotation(CommandPermissions.class);
        this.name = this.getClass().getSimpleName().toLowerCase().replace(COMMAND_PREFIX.toLowerCase(), "");
        this.description = this.params.description();
        this.usage = this.params.usage();
        this.aliases = Arrays.asList(this.params.aliases().split(","));
        this.level = this.perms.level();
        this.source = this.perms.source();
    }

    public void register()
    {
        PCommand command = new PCommand(this.name);
        command.setDescription(this.description);
        command.setUsage(this.usage);
        command.setAliases(this.aliases);
        this.getCommandMap().register("", command);
        command.setExecutor(this);
    }

    protected CommandMap getCommandMap()
    {
        if (COMMAND_MAP == null)
        {
            try
            {
                final Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
                f.setAccessible(true);
                COMMAND_MAP = (CommandMap) f.get(Bukkit.getServer());
                return getCommandMap();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
            return COMMAND_MAP;
        return getCommandMap();
    }

    private static class PCommand extends Command
    {
        private PlexCommand command = null;

        private PCommand(String name)
        {
            super(name);
        }

        public void setExecutor(PlexCommand command)
        {
            this.command = command;
        }

        @Override
        public boolean execute(CommandSender sender, String c, String[] args)
        {
            if (command == null)
                return false;
            return command.onCommand(sender, this, c, args);
        }

        @Override
        public List<String> tabComplete(CommandSender sender, String alias, String[] args)
        {
            if (command == null)
                return null;
            return Objects.requireNonNull(command.onTabComplete(sender, this, alias, args));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String c, String[] args)
    {
        return false;
    }
}
