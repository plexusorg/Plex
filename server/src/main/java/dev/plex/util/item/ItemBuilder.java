package dev.plex.util.item;

import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;

public class ItemBuilder
{

    private final ItemStack itemStack;
    private final ItemMeta meta;

    public ItemBuilder(Material material)
    {
        this.itemStack = new ItemStack(Bukkit.getItemFactory().getItemMeta(material) == null ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : material);
        this.meta = itemStack.getItemMeta();
    }

    public ItemBuilder lore(Component... lore)
    {
        this.meta.lore(Arrays.asList(lore));
        return this;
    }

    public ItemBuilder lore(String... lore)
    {
        return this.lore(Arrays.stream(lore).map(SafeMiniMessage::mmDeserialize).toArray(Component[]::new));
    }

    public ItemBuilder displayName(Component displayName)
    {
        this.meta.displayName(displayName);
        return this;
    }

    public ItemBuilder displayName(String displayName)
    {
        return this.displayName(SafeMiniMessage.mmDeserialize(displayName));
    }

    public ItemBuilder addEnchantment(Enchantment enchantment, int level)
    {
        this.meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder addItemFlag(ItemFlag... flags)
    {
        this.meta.addItemFlags(flags);
        return this;
    }

    public ItemBuilder addAttributeModifier(Attribute attribute, AttributeModifier modifier)
    {
        this.meta.addAttributeModifier(attribute, modifier);
        return this;
    }

    public ItemBuilder owner(OfflinePlayer player)
    {
        if (this.meta instanceof SkullMeta skullMeta)
        {
            skullMeta.setOwningPlayer(player);
        }
        return this;
    }

    public ItemStack build()
    {
        this.itemStack.setItemMeta(this.meta);
        return this.itemStack;
    }

}
