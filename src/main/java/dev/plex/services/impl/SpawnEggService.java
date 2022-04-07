package dev.plex.services.impl;

import dev.plex.listener.impl.SpawnEggListener;
import dev.plex.services.AbstractService;
import org.bukkit.Material;

import java.util.Arrays;

public class SpawnEggService extends AbstractService
{
    public SpawnEggService() {
        super(false, true);
    }

    @Override
    public void run() {
        SpawnEggListener.SPAWN_EGGS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SPAWN_EGG")).toList();
    }

    @Override
    public int repeatInSeconds() {
        return 0;
    }
}
