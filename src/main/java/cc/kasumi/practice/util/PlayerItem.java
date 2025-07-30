package cc.kasumi.practice.util;

import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@AllArgsConstructor
public enum PlayerItem {

    KIT_EDITOR(new ItemBuilder(
            Material.BOOK).
            name(CC.GOLD + "Edit Kits").
            lore(CC.YELLOW + "Right click to edit your kits")),

    RANKED_QUEUE(new ItemBuilder(
            Material.DIAMOND_SWORD).
            name(CC.GREEN + "Ranked Queue").
            lore(CC.YELLOW + "Right click to join a queue")),

    UNRANKED_QUEUE(new ItemBuilder(
            Material.IRON_SWORD).
            name(CC.BLUE + "Unranked Queue").
            lore(CC.YELLOW + "Right click to join a queue")),

    LEAVE_QUEUE(new ItemBuilder(
            Material.REDSTONE).
            name(CC.RED + "Leave Queue").
            lore(CC.YELLOW + "Right click to leave the queue"));

    private ItemBuilder builder;

    public ItemStack getItem() {
        return this.builder.build();
    }
}
