package cc.kasumi.practice.game.match.cache;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.menu.button.EmptyButton;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.commons.util.TimeUtil;
import cc.kasumi.practice.util.MathUtil;
import cc.kasumi.practice.util.StringUtil;
import lombok.Getter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;

@Getter
public class CachedInventory extends Menu {

    private List<String> effects = new ArrayList<>();

    private String playerName;
    private CachedInventoryType type;
    private boolean playerDead;

    private ItemStack[] contents;
    private ItemStack[] armorContents;

    private double health;
    private int foodLevel;

    public CachedInventory(Player player, CachedInventoryType type, boolean playerDead) {
        PlayerInventory inventory = player.getInventory();

        this.playerName = player.getName();
        this.type = type;
        this.playerDead = playerDead;
        this.contents = inventory.getContents().clone();
        this.armorContents = inventory.getArmorContents().clone();
        this.health = player.getHealth();
        this.foodLevel = player.getFoodLevel();

        player.getActivePotionEffects().forEach(effect -> this.effects.add(ChatColor.GREEN + StringUtil.toLowerCaseFirstUpper(effect.getType().getName().split("_")) + (effect.getAmplifier() + 1 <= 1 ? "" : " II") + ChatColor.YELLOW + " (" + TimeUtil.getTimeFormatted((effect.getDuration() / 20) * 1000L) + ")"));
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (int i = 9; i <= 35; ++i) {
            ItemStack itemStack = this.contents[i];

            if (itemStack == null) {
                continue;
            }

            buttons.put(i - 9, new EmptyButton(this.contents[i]));
        }

        for (int i = 0; i <= 8; ++i) {
            ItemStack itemStack = this.contents[i];

            if (itemStack == null) {
                continue;
            }

            buttons.put(i + 27, new EmptyButton(itemStack));
        }

        ItemStack helmet = this.armorContents[3];
        ItemStack chest = this.armorContents[2];
        ItemStack leggings = this.armorContents[1];
        ItemStack boots = this.armorContents[0];

        if (helmet != null) {
            buttons.put(36, new EmptyButton(helmet));
        }

        if (chest != null) {
            buttons.put(37, new EmptyButton(chest));
        }

        if (chest != null) {
            buttons.put(38, new EmptyButton(leggings));
        }

        if (chest != null) {
            buttons.put(39, new EmptyButton(boots));
        }

        if (this.playerDead) {
            buttons.put(48, new EmptyButton(new ItemBuilder(
                    Material.SKULL_ITEM).
                    name(CC.RED + "Player Died").
                    build()));
        } else {
            buttons.put(48, new EmptyButton(new ItemBuilder(
                    Material.SPECKLED_MELON).
                    name(CC.GREEN + "Health: " + CC.YELLOW + "" + MathUtil.round(this.health / 2D, 1) + " / 10❤").
                    build()));
        }

        buttons.put(49, new EmptyButton(new ItemBuilder(
                Material.COOKED_BEEF).
                name(CC.GREEN + "Hunger: " + CC.YELLOW + "" + (double) this.foodLevel / 2D + " / 10").
                flag(ItemFlag.HIDE_ATTRIBUTES).
                build()));

        buttons.put(50, new EmptyButton(new ItemBuilder(
                Material.BREWING_STAND_ITEM).
                name(CC.BLUE + "Potion Effects: ").
                lore(this.effects).
                build()));

        if (this.type == CachedInventoryType.POTION) {
            final short potionID = (short) 16421;

            if (this.contents != null) {
                int amount = 0;
                for (ItemStack itemStack : this.contents) {
                    if (itemStack == null) {
                        continue;
                    }

                    if (itemStack.getType() != Material.POTION || itemStack.getDurability() != potionID) {
                        continue;
                    }

                    amount++;
                }

                ItemStack itemStack = new ItemBuilder(
                        Material.POTION, amount, potionID).
                        name(ChatColor.GREEN + "Health Potions: " + ChatColor.YELLOW + amount).
                        flag(ItemFlag.HIDE_POTION_EFFECTS).
                        build();

                buttons.put(51, new EmptyButton(itemStack));
            }
        }

        return buttons;
    }

    @Override
    public String getTitle(Player player) {
        return MAIN_COLOR + this.playerName;
    }

    public static TextComponent getInventoryMessage(UUID uuid, String playerName) {
        TextComponent inventoryComponent = new TextComponent(net.md_5.bungee.api.ChatColor.YELLOW + playerName);
        ComponentBuilder inventoryBuilder = new ComponentBuilder("Click to view inventory").color(net.md_5.bungee.api.ChatColor.GOLD);

        inventoryComponent.setColor(net.md_5.bungee.api.ChatColor.YELLOW);
        inventoryComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, inventoryBuilder.create()));
        inventoryComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/viewinv " + uuid));

        return inventoryComponent;
    }
}
