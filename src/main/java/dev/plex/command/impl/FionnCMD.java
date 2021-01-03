package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.util.PlexUtils;
import dev.plex.world.BlockMapChunkGenerator;
import dev.plex.world.CustomWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Strider;
import org.bukkit.scheduler.BukkitRunnable;

@CommandParameters(description = "Subliminal message.")
@CommandPermissions(source = RequiredCommandSource.IN_GAME)
public class FionnCMD extends PlexCommand
{
    public static boolean ENABLED = false;
    public static Map<Player, Location> LOCATION_CACHE = new HashMap<>();

    public FionnCMD()
    {
        super("fionn");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (!sender.getPlayer().getUniqueId().equals(UUID.fromString("9aa3eda6-c271-440a-a578-a952ee9aee2f")))
        {
            throw new CommandFailException(tl("noPermission"));
        }
        if (args.length != 0)
        {
            throw new CommandArgumentException();
        }
        String name = "fionn";
        LinkedHashMap<Material, Integer> map = new LinkedHashMap<>();
        map.put(Material.CRIMSON_NYLIUM, 1);
        map.put(Material.BEDROCK, 1);
        World fionnWorld = new CustomWorld(name, new BlockMapChunkGenerator(map)).generate();
        ENABLED = true;
        fionnWorld.setTime(0);
        fionnWorld.getBlockAt(0, 5, 0).setType(Material.BARRIER);
        Strider fionn = (Strider)fionnWorld.spawnEntity(new Location(fionnWorld, 12, 6, 6, -180, -3), EntityType.STRIDER);
        fionn.setCustomNameVisible(true);
        fionn.setCustomName(ChatColor.GREEN + "fionn");
        fionn.setAI(false);
        Enderman elmon = (Enderman)fionnWorld.spawnEntity(new Location(fionnWorld, 12, 6, 0, 0, 18), EntityType.ENDERMAN);
        elmon.setCustomNameVisible(true);
        elmon.setCustomName(ChatColor.RED + "elmon");
        elmon.setInvulnerable(true);
        elmon.setAware(false);
        elmon.setGravity(true);
        // platforms in cage
        PlexUtils.setBlocks(new Location(fionnWorld, 10, 5, -2), new Location(fionnWorld, 14, 5, 2), Material.SMOOTH_STONE);
        PlexUtils.setBlocks(new Location(fionnWorld, 10, 9, -2), new Location(fionnWorld, 14, 9, 2), Material.SMOOTH_STONE_SLAB);
        // iron bars of cage
        PlexUtils.setBlocks(new Location(fionnWorld, 10, 8, -2), new Location(fionnWorld, 10, 6, 2), Material.IRON_BARS);
        PlexUtils.setBlocks(new Location(fionnWorld, 14, 8, 2), new Location(fionnWorld, 10, 6, 2), Material.IRON_BARS);
        PlexUtils.setBlocks(new Location(fionnWorld, 14, 8, 2), new Location(fionnWorld, 14, 6, -2), Material.IRON_BARS);
        PlexUtils.setBlocks(new Location(fionnWorld, 10, 8, -2), new Location(fionnWorld, 14, 6, -2), Material.IRON_BARS);
        // lava
        PlexUtils.setBlocks(new Location(fionnWorld, 10, 1, -2), new Location(fionnWorld, 14, 0, 2), Material.LAVA);

        // iron bars of platform
        PlexUtils.setBlocks(new Location(fionnWorld, 12, 2, 6), new Location(fionnWorld, 12, 5, 6), Material.IRON_BARS);
        // platform
        PlexUtils.setBlocks(new Location(fionnWorld, 11, 6, 7), new Location(fionnWorld, 13, 6, 5), Material.SMOOTH_STONE_SLAB);
        for (Player p : Bukkit.getOnlinePlayers())
        {
            p.setInvisible(true);
            LOCATION_CACHE.put(p, p.getLocation());
            p.teleport(new Location(fionnWorld, 0, 5, 0, -90, 0));
            PlayerCache.getPunishedPlayer(p.getUniqueId()).setFrozen(true);
        }
        lateFakeChat("elmon", "fionn! i'm sorry for not being your sex slave...", ChatColor.RED, 20);
        lateFakeChat("fionn", "it's too late for that now...", ChatColor.GREEN, 60);
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                PlexUtils.setBlocks(new Location(fionnWorld, 13, 5, -1), new Location(fionnWorld, 11, 5, 1), Material.AIR);
            }
        }.runTaskLater(plugin, 100);
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                fionn.teleport(new Location(fionnWorld, 2.5, 5.5, 0, 90, -10));
            }
        }.runTaskLater(plugin, 160);
        new BukkitRunnable()
        {
            public void run()
            {
                fionn.remove();
                elmon.remove();
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    p.setInvisible(false);
                    Location location = LOCATION_CACHE.get(p);
                    if (location != null)
                    {
                        p.teleport(location);
                    }
                    PlayerCache.getPunishedPlayer(p.getUniqueId()).setFrozen(false);
                }
                LOCATION_CACHE.clear();
                ENABLED = false;
            }
        }.runTaskLater(plugin, 200);
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return ImmutableList.of();
    }

    public static void lateFakeChat(String name, String message, ChatColor color, int delay)
    {
        new BukkitRunnable()
        {
            public void run()
            {
                Bukkit.broadcastMessage(color + name + ChatColor.GRAY + ": " + ChatColor.WHITE + message);
            }
        }.runTaskLater(plugin, delay);
    }
}