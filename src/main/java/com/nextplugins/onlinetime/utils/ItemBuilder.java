package com.nextplugins.onlinetime.utils;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ItemBuilder {

    private final ItemStack item;

    public ItemBuilder(ItemStack item) {
        this.item = item;
    }

    public ItemBuilder(Material type) {
        this(new ItemStack(type));
    }

    public ItemBuilder(Material type, int quantity, short data) {
        this(new ItemStack(type, quantity, data));
    }

    public ItemBuilder(String name) {

        item = TypeUtil.convertFromLegacy("SKULL_ITEM", 3);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwner(name);

        item.setItemMeta(meta);
    }

    public ItemBuilder changeItemMeta(Consumer<ItemMeta> consumer) {
        ItemMeta itemMeta = item.getItemMeta();
        consumer.accept(itemMeta);
        item.setItemMeta(itemMeta);
        return this;
    }

    public ItemBuilder changeItem(Consumer<ItemStack> consumer) {
        consumer.accept(item);
        return this;
    }

    public ItemBuilder glow() {

        changeItemMeta(meta -> {
            meta.addEnchant(Enchantment.DAMAGE_ALL, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        });

        return this;
    }

    public ItemBuilder name(String name) {
        return changeItemMeta(it -> it.setDisplayName(ColorUtil.colored(name)));
    }

    public ItemBuilder setLore(String... lore) {
        return changeItemMeta(it -> it.setLore(Arrays.asList(ColorUtil.colored(lore))));
    }

    public ItemBuilder setLore(List<String> lore) {
        return changeItemMeta(
                it -> it.setLore(lore.stream().map(ColorUtil::colored).collect(Collectors.toList())));
    }

    public ItemBuilder addLore(List<String> lore) {
        if (lore == null || lore.isEmpty()) return this;

        return changeItemMeta(meta -> {
            List<String> list = meta.getLore();
            list.addAll(lore);
            meta.setLore(list);
        });
    }

    public ItemStack wrap() {
        return item;
    }
}