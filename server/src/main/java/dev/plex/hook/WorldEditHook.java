package dev.plex.hook;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extension.platform.Actor;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.World;
import dev.plex.Plex;
import dev.plex.world.WorldModificationPolicy;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;

public final class WorldEditHook implements Listener
{
    private final Plex plugin;
    private final WorldModificationPolicy policy;

    public WorldEditHook(Plex plugin)
    {
        this.plugin = plugin;
        this.policy = plugin.getWorldModificationPolicy();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        WorldEdit.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onEditSession(EditSessionEvent event)
    {
        Actor actor = event.getActor();
        if (event.getStage() != EditSession.Stage.BEFORE_CHANGE || event.getWorld() == null || actor == null || !actor.isPlayer())
        {
            return;
        }
        if (policy.denyModification(Bukkit.getPlayer(actor.getUniqueId()), event.getWorld().getName()))
        {
            event.setCancelled(true);
        }
    }

    public static String selectionWorldName(String playerName)
    {
        LocalSession session = WorldEdit.getInstance().getSessionManager().findByName(playerName);
        if (session == null)
        {
            return null;
        }
        World world = session.getSelectionWorld();
        return world == null ? null : world.getName();
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent event)
    {
        if (event.getPlugin() == plugin)
        {
            WorldEdit.getInstance().getEventBus().unregister(this);
        }
    }
}
