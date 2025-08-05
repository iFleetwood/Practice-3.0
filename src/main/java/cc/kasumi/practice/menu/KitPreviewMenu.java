package cc.kasumi.practice.menu;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.menu.button.EmptyButton;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.practice.game.ladder.Ladder;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

@RequiredArgsConstructor
public class KitPreviewMenu extends Menu {

    private final Ladder ladder;
    private final PlayerInv kit;
    private final boolean isCustomKit;

    @Override
    public String getTitle(Player player) {
        return MAIN_COLOR + (isCustomKit ? "Custom Kit: " : "Default Kit: ") + ladder.getDisplayName();
    }

    @Override
    public int getSize() {
        return 54;
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        // Display main inventory (slots 9-35 in player inventory = slots 0-26 in menu)
        for (int i = 9; i <= 35; i++) {
            ItemStack item = kit.getContents()[i];
            if (item != null) {
                buttons.put(i - 9, new EmptyButton(item));
            }
        }

        // Display hotbar (slots 0-8 in player inventory = slots 27-35 in menu)
        for (int i = 0; i <= 8; i++) {
            ItemStack item = kit.getContents()[i];
            if (item != null) {
                buttons.put(i + 27, new EmptyButton(item));
            }
        }

        // Display armor
        ItemStack[] armor = kit.getArmorContents();
        if (armor[3] != null) buttons.put(36, new EmptyButton(armor[3])); // Helmet
        if (armor[2] != null) buttons.put(37, new EmptyButton(armor[2])); // Chestplate
        if (armor[1] != null) buttons.put(38, new EmptyButton(armor[1])); // Leggings
        if (armor[0] != null) buttons.put(39, new EmptyButton(armor[0])); // Boots

        // Info button
        buttons.put(49, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.PAPER)
                        .name(CC.AQUA + "Kit Information")
                        .lore(Arrays.asList(
                                "",
                                CC.WHITE + "Ladder: " + CC.YELLOW + ladder.getDisplayName(),
                                CC.WHITE + "Kit Type: " + CC.YELLOW + (isCustomKit ? "Custom" : "Default"),
                                CC.WHITE + "Editable: " + (ladder.isEditable() ? CC.GREEN + "Yes" : CC.RED + "No"),
                                "",
                                CC.GRAY + "This is a preview of the kit",
                                CC.GRAY + "that will be used in matches"
                        ))
                        .build();
            }
        });

        // Back button
        buttons.put(45, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(CC.YELLOW + "Back")
                        .lore(CC.YELLOW + "Return to kit editor")
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                new KitEditingMenu(ladder).openMenu(player);
            }
        });

        // Fill empty slots with glass
        for (int i = 40; i < 45; i++) {
            buttons.put(i, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.STAINED_GLASS_PANE)
                            .durability((short) 7)
                            .name(" ")
                            .build();
                }
            });
        }

        for (int i = 46; i < 54; i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.STAINED_GLASS_PANE)
                                .durability((short) 7)
                                .name(" ")
                                .build();
                    }
                });
            }
        }

        return buttons;
    }
}