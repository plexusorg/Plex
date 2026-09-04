package dev.plex.util;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Bukkit;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import io.papermc.paper.ServerBuildInfo;
import dev.plex.Plex;
import dev.plex.api.message.MessageFormatter;
import dev.plex.api.message.MessagePlaceholder;
import dev.plex.config.Config;
import dev.plex.listener.impl.ChatListener;
import dev.plex.util.minimessage.SafeMiniMessage;

import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PlexUtils
{
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static Config config;
    private static Config messages;

    public static void configure(Config config, Config messages)
    {
        PlexUtils.config = config;
        PlexUtils.messages = messages;
    }

    public static List<String> DEVELOPERS =
            Arrays.asList("78408086-1991-4c33-a571-d8fa325465b2", // Telesphoreo
                    "f5cd54c4-3a24-4213-9a56-c06c49594dff", // Taahh
                    "53b1512e-3481-4702-9f4f-63cb9c8be6a1", // supernt
                    "ca83b658-c03b-4106-9edc-72f70a80656d", // ayunami2000
                    "2e06e049-24c8-42e4-8bcf-d35372af31e6", // Fleek
                    "a52f1f08-a398-400a-bca4-2b74b81feae6" // Allink
            );

    private static final Pattern LEGACY_FORMATTING_PATTERN = Pattern.compile(".*(?i)(([§&])((#[a-f0-9]{3,6})|([0-9a-fklmnor]))).*");

    public static void disabledEffect(Player player, Location location)
    {
        Particle.CLOUD.builder().location(location).receivers(player).extra(0).offset(0.5, 0.5, 0.5).count(5).spawn();
        Particle.FLAME.builder().location(location).receivers(player).extra(0).offset(0.5, 0.5, 0.5).count(3).spawn();
        Particle.SOUL_FIRE_FLAME.builder().location(location).receivers(player).offset(0.5, 0.5, 0.5).extra(0).count(2).spawn();
        player.playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
    }

    public static void disabledEffectMultiple(Player[] players, Location location)
    {
        if (players.length < 1)
        {
            return;
        }

        Particle.CLOUD.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(5).spawn();
        Particle.FLAME.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(3).spawn();
        Particle.SOUL_FIRE_FLAME.builder().location(location).receivers(players).offset(0.5, 0.5, 0.5).extra(0).count(2)
                .spawn();
        // note that the sound is played to everyone who is close enough to hear it
        players[0].getWorld().playSound(location, Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
    }

    public static boolean hasVanishPlugin()
    {
        return Bukkit.getPluginManager().isPluginEnabled("SuperVanish") || Bukkit.getPluginManager().isPluginEnabled("PremiumVanish");
    }

    public static boolean isFolia()
    {
        return ServerBuildInfo.buildInfo().isBrandCompatible(Key.key("papermc", "folia"));
    }

    public static Component stringToComponent(String input)
    {
        input = cleanString(input);

        return LEGACY_FORMATTING_PATTERN.matcher(input).find() ?
                LegacyComponentSerializer.legacyAmpersand().deserialize(input.replaceAll("([§&]+)(k+)", "") // Ugly hack, but it tries to prevent &k and any attempts to bypass it.
                ).decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE) :
                SafeMiniMessage.mmDeserializeWithoutEvents(input);
    }

    public static String mmStripColor(String input)
    {
        return PlainTextComponentSerializer.plainText().serialize(mmDeserialize(input));
    }

    public static Component mmDeserialize(String input)
    {
        boolean aprilFools = true; // true by default
        if (config != null && config.contains("april_fools"))
        {
            aprilFools = config.getBoolean("april_fools");
        }
        ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
        if (aprilFools && date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1)
        {
            Component component = MINI_MESSAGE.deserialize(input); // removes existing tags
            return MINI_MESSAGE.deserialize("<rainbow>" + PlainTextComponentSerializer.plainText().serialize(component));
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static String mmSerialize(Component input)
    {
        return MINI_MESSAGE.serialize(input);
    }

    public static Component mmCustomDeserialize(String input, TagResolver... resolvers)
    {
        return MiniMessage.builder().tags(TagResolver.builder().resolvers(resolvers).build()).build().deserialize(input);
    }

    public static Component messageComponent(String entry, MessagePlaceholder... placeholders)
    {
        return MessageFormatter.formatComponent(messageString(entry), placeholders);
    }

    public static String messageString(String entry, MessagePlaceholder... placeholders)
    {
        String message = messages.getString(entry);
        if (message == null)
        {
            throw new NullPointerException();
        }
        return MessageFormatter.formatString(message, placeholders);
    }


    public static String getTextFromComponent(Component component)
    {
        try
        {
            return ((TextComponent) component).content();
        }
        catch (Exception e)
        {
            PlexLog.warn("Unable to get text of component", e.getLocalizedMessage());
            return "";
        }
    }

    public static List<String> getPlayerNameList()
    {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public static void broadcast(String message)
    {
        Bukkit.broadcast(MINI_MESSAGE.deserialize(message));
    }

    public static void broadcast(Component component)
    {
        Bukkit.broadcast(component);
    }

    public static void broadcastToAdmins(Component component, String permission)
    {
        Bukkit.broadcast(component, permission);
    }

    public static List<UUID> adminChat(String senderName, String prefix, String message, UUID... ignore)
    {
        List<UUID> sent = Lists.newArrayList();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (Arrays.stream(ignore).anyMatch(uuid -> player.getUniqueId().equals(uuid)))
            {
                continue;
            }
            if (player.hasPermission("plex.adminchat"))
            {
                player.sendMessage(PlexUtils.messageComponent("adminChatFormat", placeholder("sender", senderName), placeholder("prefix", prefix), placeholder("message", message)).replaceText(ChatListener.URL_REPLACEMENT_CONFIG));
                sent.add(player.getUniqueId());
            }
        }
        return sent;
    }

    public static String cleanString(String input)
    {
        return CharMatcher.forPredicate(c -> Character.getDirectionality(c) != Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE && Character.getDirectionality(c) != Character.DIRECTIONALITY_RIGHT_TO_LEFT).retainFrom(input);
    }
}
