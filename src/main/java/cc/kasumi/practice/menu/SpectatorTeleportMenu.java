package cc.kasumi.practice.spectator;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.practice.util.SpectatorUtil;
import lombok.AllArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectatorTeleportMenu extends Menu {

    @Override
    public String getTitle(Player player) {
        return CC.GOLD + "Teleport to Player";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        List<Player> teleportablePlayers = SpectatorUtil.getTeleportablePlayers(player);

        if (teleportablePlayers.isEmpty()) {
            buttons.put(13, new Button() {
                @Override
                public ItemStack getButtonItem(Player player) {
                    return new ItemBuilder(Material.BARRIER)
                            .name(CC.RED + "No players available")
                            .lore(CC.GRAY + "There are no players to teleport to")
                            .build();
                }
            });
            return buttons;
        }

        int slot = 0;
        for (Player teleportPlayer : teleportablePlayers) {
            if (slot >= 45) break; // Don't overflow the inventory

            buttons.put(slot, new TeleportButton(teleportPlayer));
            slot++;
        }

        return buttons;
    }

    @AllArgsConstructor
    private static class TeleportButton extends Button {

        private final Player targetPlayer;

        @Override
        public ItemStack getButtonItem(Player player) {
            return new ItemBuilder(Material.SKULL_ITEM)
                    .name(CC.YELLOW + targetPlayer.getName())
                    .lore(
                            CC.GRAY + "Health: " + CC.RED + String.format("%.1f", targetPlayer.getHealth()) + "/20.0❤",
                            CC.GRAY + "Food: " + CC.GREEN + targetPlayer.getFoodLevel() + "/20",
                            "",
                            CC.YELLOW + "Click to teleport!"
                    )
                    .build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            player.closeInventory();
            SpectatorUtil.teleportToPlayer(player, targetPlayer);
        }
    }
}