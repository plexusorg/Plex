package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GameruleService extends AbstractService {
    private final Map<GameRule, Boolean> rules = new EnumMap<>(GameRule.class);

    public GameruleService() {
        super(false, false);
    }

    public void setGameRule(GameRule gameRule, boolean value) {
        setGameRule(gameRule, value, true);
    }

    public void setGameRule(GameRule gameRule, boolean value, boolean doCommit) {
        rules.put(gameRule, value);
        if (doCommit) {
            commitGameRules();
        }
    }

    @SuppressWarnings("deprecation")
    public void commitGameRules() {
        List<World> worlds = Bukkit.getWorlds();
        for (Map.Entry<GameRule, Boolean> gameRuleEntry : rules.entrySet()) {
            String gameRuleName = gameRuleEntry.getKey().getGameRuleName();
            String gameRuleValue = gameRuleEntry.getValue().toString();

            for (World world : worlds) {
                world.setGameRuleValue(gameRuleName, gameRuleValue);
                if (gameRuleEntry.getKey() == GameRule.DO_DAYLIGHT_CYCLE && !gameRuleEntry.getValue()) {
                    long time = world.getTime();
                    time -= time % 24000;
                    world.setTime(time + 24000 + 6000);
                }
            }
        }
    }

    @Override
    public void run() {
        for (GameRule gameRule : GameRule.values()) {
            rules.put(gameRule, gameRule.getDefaultValue());
            PlexLog.log(gameRule.toString());
        }
    }

    @Override
    public int repeatInSeconds() {
        return 0;
    }

    public enum GameRule {
        DO_FIRE_TICK("doFireTick", true),
        MOB_GRIEFING("mobGriefing", true),
        KEEP_INVENTORY("keepInventory", true),
        DO_MOB_SPAWNING("doMobSpawning", true),
        DO_MOB_LOOT("doMobLoot", true),
        DO_TILE_DROPS("doTileDrops", true),
        COMMAND_BLOCK_OUTPUT("commandBlockOutput", true),
        NATURAL_REGENERATION("naturalRegeneration", true),
        DO_DAYLIGHT_CYCLE("doDaylightCycle", true),
        ANNOUNCE_ADVANCEMENTS("announceAdvancements", true),
        SHOW_DEATH_MESSAGES("showDeathMessages", true),
        SEND_COMMAND_FEEDBACK("sendCommandFeedback", true);

        private final String gameRuleName;
        private final boolean defaultValue;

        GameRule(String gameRuleName, boolean defaultValue) {
            this.gameRuleName = gameRuleName;
            this.defaultValue = defaultValue;
        }

        public String getGameRuleName() {
            return gameRuleName;
        }

        public boolean getDefaultValue() {
            return defaultValue;
        }
    }
}
