package cc.kasumi.practice.menu;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.commons.util.TypeData;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class KitEditorMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return MAIN_COLOR + "Kit Editor";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Ladder ladder : Practice.getInstance().getLadders().values()) {
            if (!ladder.isEditable()) {
                continue; // Skip non-editable ladders
            }

            buttons.put(ladder.getDisplaySlot() - 1, new KitEditorButton(ladder));
        }

        return buttons;
    }

    @AllArgsConstructor
    private static class KitEditorButton extends Button {

        private final Ladder ladder;

        @Override
        public ItemStack getButtonItem(Player player) {
            TypeData typeData = ladder.getDisplayType();

            return new ItemBuilder(typeData.getType(), typeData.getData())
                    .name(MAIN_COLOR + ladder.getDisplayName())
                    .lore(Arrays.asList(
                            "",
                            CC.YELLOW + "Click to edit your kit for this ladder",
                            "",
                            CC.GRAY + "Type: " + CC.WHITE + ladder.getType().name(),
                            CC.GRAY + "Editable: " + (ladder.isEditable() ? CC.GREEN + "Yes" : CC.RED + "No"),
                            CC.GRAY + "Ranked: " + (ladder.isRanked() ? CC.GREEN + "Yes" : CC.RED + "No")
                    ))
                    .flag(ItemFlag.HIDE_POTION_EFFECTS)
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            if (!ladder.isEditable()) {
                player.sendMessage(ERROR_COLOR + "This ladder is not editable!");
                return;
            }

            player.closeInventory();
            new KitEditingMenu(ladder).openMenu(player);
        }
    }
}