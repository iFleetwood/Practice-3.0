package cc.kasumi.practice.menu;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.kit.PlayerKitManager;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
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
public class KitEditingMenu extends Menu {

    private final Ladder ladder;

    @Override
    public String getTitle(Player player) {
        return MAIN_COLOR + "Editing: " + ladder.getDisplayName();
    }

    @Override
    public int getSize() {
        return 54; // 6 rows
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();
        PlayerKitManager kitManager = Practice.getInstance().getPlayerKitManager();

        // Load default kit button
        buttons.put(48, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.CHEST)
                        .name(CC.GREEN + "Load Default Kit")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to load the default kit",
                                CC.YELLOW + "for this ladder into your inventory",
                                "",
                                CC.GRAY + "This will replace your current inventory!",
                                ladder.getDefaultKit() != null ? CC.GREEN + "Default kit available" : CC.RED + "No default kit set"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                if (ladder.getDefaultKit() == null) {
                    player.sendMessage(ERROR_COLOR + "This ladder doesn't have a default kit!");
                    return;
                }

                PlayerInv defaultKit = ladder.getDefaultKit();
                player.getInventory().clear();
                player.getInventory().setContents(defaultKit.getContents());
                player.getInventory().setArmorContents(defaultKit.getArmorContents());
                player.updateInventory();
                player.sendMessage(MAIN_COLOR + "Loaded default kit for " + SEC_COLOR + ladder.getDisplayName());
                player.closeInventory();
            }
        });

        // Load custom kit button
        buttons.put(46, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                boolean hasCustomKit = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());

                return new ItemBuilder(Material.ENDER_CHEST)
                        .name(hasCustomKit ? CC.GREEN + "Load Custom Kit" : CC.GRAY + "Load Custom Kit")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to load your custom kit",
                                CC.YELLOW + "for this ladder into your inventory",
                                "",
                                hasCustomKit ? CC.GREEN + "You have a custom kit saved!" : CC.GRAY + "No custom kit saved",
                                CC.GRAY + "This will replace your current inventory!"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                PlayerInv customKit = kitManager.getPlayerKit(player.getUniqueId(), ladder.getName());
                if (customKit == null) {
                    player.sendMessage(ERROR_COLOR + "You don't have a custom kit saved for this ladder!");
                    return;
                }

                player.getInventory().clear();
                player.getInventory().setContents(customKit.getContents());
                player.getInventory().setArmorContents(customKit.getArmorContents());
                player.updateInventory();
                player.sendMessage(MAIN_COLOR + "Loaded your custom kit for " + SEC_COLOR + ladder.getDisplayName());
                player.closeInventory();
            }
        });

        // Reset to default kit button
        buttons.put(47, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                boolean hasCustomKit = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());

                return new ItemBuilder(Material.ANVIL)
                        .name(CC.YELLOW + "Reset to Default")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to delete your custom kit",
                                CC.YELLOW + "and use the default ladder kit",
                                "",
                                hasCustomKit ? CC.GREEN + "You have a custom kit" : CC.GRAY + "Already using default",
                                CC.RED + "This cannot be undone!"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                boolean hadCustomKit = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());

                if (!hadCustomKit) {
                    player.sendMessage(ERROR_COLOR + "You don't have a custom kit to reset!");
                    return;
                }

                kitManager.removePlayerKit(player.getUniqueId(), ladder.getName());

                // Load default kit if available
                if (ladder.getDefaultKit() != null) {
                    PlayerInv defaultKit = ladder.getDefaultKit();
                    player.getInventory().clear();
                    player.getInventory().setContents(defaultKit.getContents());
                    player.getInventory().setArmorContents(defaultKit.getArmorContents());
                    player.updateInventory();
                }

                player.sendMessage(MAIN_COLOR + "Reset to default kit for " + SEC_COLOR + ladder.getDisplayName());
            }
        });

        // Clear inventory button
        buttons.put(49, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.BARRIER)
                        .name(CC.RED + "Clear Inventory")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to clear your inventory",
                                CC.YELLOW + "and armor slots",
                                "",
                                CC.RED + "This cannot be undone!"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                player.getInventory().clear();
                player.getInventory().setArmorContents(new ItemStack[4]);
                player.updateInventory();
                player.sendMessage(MAIN_COLOR + "Cleared your inventory!");
            }
        });

        // Save kit button
        buttons.put(50, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                boolean hasCustomKit = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());
                boolean hasItems = !isInventoryEmpty(player);

                return new ItemBuilder(Material.BOOK_AND_QUILL)
                        .name(CC.GOLD + "Save Kit")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to save your current inventory",
                                CC.YELLOW + "as your custom kit for this ladder",
                                "",
                                hasCustomKit ? CC.GREEN + "You have a custom kit saved!" : CC.GRAY + "Using default kit",
                                hasItems ? CC.GREEN + "Inventory has items to save" : CC.RED + "Inventory is empty!",
                                "",
                                CC.GREEN + "This will be used in matches!"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                if (isInventoryEmpty(player)) {
                    player.sendMessage(ERROR_COLOR + "Your inventory is empty! Add some items before saving.");
                    return;
                }

                kitManager.saveCurrentInventoryAsKit(player, ladder);

                player.sendMessage(MAIN_COLOR + "Saved your kit for " + SEC_COLOR + ladder.getDisplayName() + MAIN_COLOR + "!");
                player.sendMessage(SEC_COLOR + "This kit will now be used in matches for this ladder.");
            }
        });

        // Kit info button
        buttons.put(51, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                boolean hasCustomKit = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());

                return new ItemBuilder(Material.PAPER)
                        .name(CC.AQUA + "Kit Information")
                        .lore(Arrays.asList(
                                "",
                                CC.WHITE + "Ladder: " + CC.YELLOW + ladder.getDisplayName(),
                                CC.WHITE + "Type: " + CC.YELLOW + ladder.getType().name(),
                                CC.WHITE + "Editable: " + (ladder.isEditable() ? CC.GREEN + "Yes" : CC.RED + "No"),
                                "",
                                CC.WHITE + "Your Kit Status:",
                                hasCustomKit ? CC.GREEN + "✓ Using custom kit" : CC.GRAY + "✗ Using default kit",
                                "",
                                CC.GRAY + "Custom kits are saved per ladder",
                                CC.GRAY + "and used automatically in matches"
                        ))
                        .build();
            }
        });

        // Back to kit selector button
        buttons.put(45, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.ARROW)
                        .name(CC.YELLOW + "Back to Kit Selector")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to go back to the",
                                CC.YELLOW + "kit editor menu"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                player.closeInventory();
                new KitEditorMenu().openMenu(player);
            }
        });

        // Preview kit button
        buttons.put(52, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.EYE_OF_ENDER)
                        .name(CC.PINK + "Preview Kit")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to preview your current",
                                CC.YELLOW + "inventory as it would appear in matches",
                                "",
                                CC.GRAY + "This shows exactly what you'll get",
                                CC.GRAY + "when starting a match"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                if (isInventoryEmpty(player)) {
                    player.sendMessage(ERROR_COLOR + "Your inventory is empty! Nothing to preview.");
                    return;
                }

                // Create a temporary PlayerInv from current inventory
                cc.kasumi.commons.util.PlayerInv tempKit = new cc.kasumi.commons.util.PlayerInv(
                        player.getInventory().getContents().clone(),
                        player.getInventory().getArmorContents().clone()
                );

                player.closeInventory();
                new KitPreviewMenu(ladder, tempKit, true).openMenu(player);
            }
        });

        // Back to lobby button
        buttons.put(53, new Button() {
            @Override
            public ItemStack getButtonItem(Player player) {
                return new ItemBuilder(Material.REDSTONE)
                        .name(CC.RED + "Back to Lobby")
                        .lore(Arrays.asList(
                                "",
                                CC.YELLOW + "Click to return to the lobby",
                                CC.YELLOW + "and exit kit editing mode",
                                "",
                                CC.GRAY + "Your progress will be saved"
                        ))
                        .build();
            }

            @Override
            public void clicked(Player player, ClickType clickType) {
                player.closeInventory();
                returnToLobby(player);
            }
        });

        // Fill remaining bottom row with glass panes
        for (int i = 45; i < 54; i++) {
            if (!buttons.containsKey(i)) {
                buttons.put(i, new Button() {
                    @Override
                    public ItemStack getButtonItem(Player player) {
                        return new ItemBuilder(Material.STAINED_GLASS_PANE)
                                .durability((short) 7) // Gray glass pane
                                .name(" ")
                                .build();
                    }
                });
            }
        }

        return buttons;
    }

    @Override
    public void onOpen(Player player) {
        super.onOpen(player);

        // Set player to editing state
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        if (practicePlayer != null) {
            practicePlayer.setPlayerState(PlayerState.EDITING);
        }

        // Load the effective kit (custom if exists, otherwise default)
        PlayerKitManager kitManager = Practice.getInstance().getPlayerKitManager();
        PlayerInv effectiveKit = kitManager.getEffectiveKit(player, ladder);

        if (effectiveKit != null && isInventoryEmpty(player)) {
            player.getInventory().setContents(effectiveKit.getContents());
            player.getInventory().setArmorContents(effectiveKit.getArmorContents());
            player.updateInventory();

            boolean isCustom = kitManager.hasCustomKit(player.getUniqueId(), ladder.getName());
            player.sendMessage(MAIN_COLOR + "Loaded " +
                    (isCustom ? "your custom kit" : "default kit") +
                    " for " + SEC_COLOR + ladder.getDisplayName());
        }
    }

    @Override
    public void onClose(Player player) {
        super.onClose(player);

        // Don't automatically return to lobby - let them use the button
        // This allows them to close and reopen the menu without losing progress
    }

    private boolean isInventoryEmpty(Player player) {
        // Check main inventory
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }

        // Check armor slots
        for (ItemStack item : player.getInventory().getArmorContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return false;
            }
        }

        return true;
    }

    private void returnToLobby(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        if (practicePlayer != null) {
            practicePlayer.setPlayerState(PlayerState.LOBBY);
        }

        // Give lobby items
        player.getInventory().clear();
        player.getInventory().setContents(GameUtil.getLobbyContents());
        player.updateInventory();

        player.sendMessage(MAIN_COLOR + "Returned to lobby!");
    }
}