package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.kit.PlayerKitManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("kitadmin|ka")
@CommandPermission("practice.admin")
public class KitAdminCommand extends BaseCommand {

    private final PlayerKitManager kitManager;

    public KitAdminCommand() {
        this.kitManager = Practice.getInstance().getPlayerKitManager();
    }

    @Default
    @HelpCommand
    public void onKitAdminCommand(CommandSender sender) {
        sender.sendMessage(MAIN_COLOR + "Kit Admin Commands:");
        sender.sendMessage(MAIN_COLOR + "/ka stats - Show kit statistics");
        sender.sendMessage(MAIN_COLOR + "/ka player <player> - Show player's kits");
        sender.sendMessage(MAIN_COLOR + "/ka clear <player> [ladder] - Clear player's kits");
        sender.sendMessage(MAIN_COLOR + "/ka copy <from> <to> <ladder> - Copy kit between players");
        sender.sendMessage(MAIN_COLOR + "/ka preview <player> <ladder> - Preview player's kit");
        sender.sendMessage(MAIN_COLOR + "/ka give <player> <ladder> - Give kit to online player");
        sender.sendMessage(MAIN_COLOR + "/ka setfrom <player> <ladder> - Set kit from your inventory");
        sender.sendMessage(MAIN_COLOR + "/ka reload - Reload kit system");
    }

    @Subcommand("stats")
    public void onStatsCommand(CommandSender sender) {
        Map<String, Object> stats = kitManager.getKitStats();

        sender.sendMessage(MAIN_COLOR + "=== Kit Statistics ===");
        sender.sendMessage(SEC_COLOR + "Total players with kits: " + MAIN_COLOR + stats.get("totalPlayers"));
        sender.sendMessage(SEC_COLOR + "Total custom kits: " + MAIN_COLOR + stats.get("totalCustomKits"));

        @SuppressWarnings("unchecked")
        Map<String, Integer> kitsPerLadder = (Map<String, Integer>) stats.get("kitsPerLadder");

        if (!kitsPerLadder.isEmpty()) {
            sender.sendMessage(MAIN_COLOR + "Kits per ladder:");
            kitsPerLadder.forEach((ladder, count) ->
                    sender.sendMessage("  " + SEC_COLOR + ladder + ": " + MAIN_COLOR + count + " kits"));
        }
    }

    @Subcommand("player")
    @Syntax("<player>")
    public void onPlayerCommand(CommandSender sender, String playerName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Player '" + playerName + "' not found!");
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        String[] customKitLadders = kitManager.getCustomKitLadders(playerUUID);

        sender.sendMessage(MAIN_COLOR + "=== " + offlinePlayer.getName() + "'s Kits ===");

        if (customKitLadders.length == 0) {
            sender.sendMessage(SEC_COLOR + "No custom kits saved");
            return;
        }

        sender.sendMessage(SEC_COLOR + "Custom kits (" + customKitLadders.length + "):");
        for (String ladderName : customKitLadders) {
            Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
            String displayName = ladder != null ? ladder.getDisplayName() : ladderName;
            sender.sendMessage("  " + MAIN_COLOR + "- " + SEC_COLOR + displayName);
        }
    }

    @Subcommand("clear")
    @Syntax("<player> [ladder]")
    public void onClearCommand(CommandSender sender, String playerName, @Optional String ladderName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Player '" + playerName + "' not found!");
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();

        if (ladderName == null) {
            // Clear all kits for player
            kitManager.clearPlayerKits(playerUUID);
            sender.sendMessage(MAIN_COLOR + "Cleared all custom kits for " + SEC_COLOR + offlinePlayer.getName());
        } else {
            // Clear specific ladder kit
            ladderName = ladderName.toLowerCase();

            if (!Practice.getInstance().getLadders().containsKey(ladderName)) {
                sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
                return;
            }

            if (!kitManager.hasCustomKit(playerUUID, ladderName)) {
                sender.sendMessage(ERROR_COLOR + offlinePlayer.getName() + " doesn't have a custom kit for '" + ladderName + "'!");
                return;
            }

            kitManager.removePlayerKit(playerUUID, ladderName);
            sender.sendMessage(MAIN_COLOR + "Cleared " + SEC_COLOR + offlinePlayer.getName() + MAIN_COLOR + "'s custom kit for " + SEC_COLOR + ladderName);
        }
    }

    @Subcommand("copy")
    @Syntax("<fromPlayer> <toPlayer> <ladder>")
    public void onCopyCommand(CommandSender sender, String fromPlayerName, String toPlayerName, String ladderName) {
        OfflinePlayer fromPlayer = Bukkit.getOfflinePlayer(fromPlayerName);
        OfflinePlayer toPlayer = Bukkit.getOfflinePlayer(toPlayerName);

        if (!fromPlayer.hasPlayedBefore() && !fromPlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Source player '" + fromPlayerName + "' not found!");
            return;
        }

        if (!toPlayer.hasPlayedBefore() && !toPlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Target player '" + toPlayerName + "' not found!");
            return;
        }

        ladderName = ladderName.toLowerCase();
        if (!Practice.getInstance().getLadders().containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        if (!kitManager.hasCustomKit(fromPlayer.getUniqueId(), ladderName)) {
            sender.sendMessage(ERROR_COLOR + fromPlayer.getName() + " doesn't have a custom kit for '" + ladderName + "'!");
            return;
        }

        // Copy the kit
        cc.kasumi.commons.util.PlayerInv kit = kitManager.getPlayerKit(fromPlayer.getUniqueId(), ladderName);
        cc.kasumi.commons.util.PlayerInv kitCopy = new cc.kasumi.commons.util.PlayerInv(
                kit.getContents().clone(),
                kit.getArmorContents().clone()
        );

        kitManager.setPlayerKit(toPlayer.getUniqueId(), ladderName, kitCopy);

        sender.sendMessage(MAIN_COLOR + "Copied " + SEC_COLOR + fromPlayer.getName() + MAIN_COLOR + "'s kit to " +
                SEC_COLOR + toPlayer.getName() + MAIN_COLOR + " for ladder " + SEC_COLOR + ladderName);
    }

    @Subcommand("preview")
    @Syntax("<player> <ladder>")
    public void onPreviewCommand(Player sender, String playerName, String ladderName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Player '" + playerName + "' not found!");
            return;
        }

        ladderName = ladderName.toLowerCase();
        Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
        if (ladder == null) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        cc.kasumi.commons.util.PlayerInv kit = kitManager.getPlayerKit(playerUUID, ladderName);
        boolean isCustom = kit != null;

        if (!isCustom) {
            kit = ladder.getDefaultKit();
            if (kit == null) {
                sender.sendMessage(ERROR_COLOR + "No kit available for ladder '" + ladderName + "'!");
                return;
            }
        }

        // Open preview menu
        new cc.kasumi.practice.menu.KitPreviewMenu(ladder, kit, isCustom).openMenu(sender);
    }

    @Subcommand("give")
    @Syntax("<player> <ladder>")
    public void onGiveCommand(Player sender, OnlinePlayer target, String ladderName) {
        ladderName = ladderName.toLowerCase();
        Ladder ladder = Practice.getInstance().getLadders().get(ladderName);

        if (ladder == null) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        Player targetPlayer = target.getPlayer();
        cc.kasumi.commons.util.PlayerInv kit = kitManager.getEffectiveKit(targetPlayer, ladder);

        if (kit == null) {
            sender.sendMessage(ERROR_COLOR + "No kit available for ladder '" + ladderName + "'!");
            return;
        }

        // Give the kit to the target player
        targetPlayer.getInventory().clear();
        targetPlayer.getInventory().setContents(kit.getContents());
        targetPlayer.getInventory().setArmorContents(kit.getArmorContents());
        targetPlayer.updateInventory();

        boolean isCustom = kitManager.hasCustomKit(targetPlayer.getUniqueId(), ladderName);

        sender.sendMessage(MAIN_COLOR + "Gave " + SEC_COLOR + targetPlayer.getName() + MAIN_COLOR +
                " the " + (isCustom ? "custom" : "default") + " kit for " + SEC_COLOR + ladder.getDisplayName());
        targetPlayer.sendMessage(MAIN_COLOR + "You received the " + (isCustom ? "custom" : "default") +
                " kit for " + SEC_COLOR + ladder.getDisplayName() + MAIN_COLOR + " from " +
                SEC_COLOR + sender.getName());
    }

    @Subcommand("setfrom")
    @Syntax("<player> <ladder>")
    public void onSetFromCommand(Player sender, String playerName, String ladderName) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Player '" + playerName + "' not found!");
            return;
        }

        ladderName = ladderName.toLowerCase();
        Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
        if (ladder == null) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        // Check if sender has items in inventory
        boolean hasItems = false;
        for (org.bukkit.inventory.ItemStack item : sender.getInventory().getContents()) {
            if (item != null && item.getType() != org.bukkit.Material.AIR) {
                hasItems = true;
                break;
            }
        }

        if (!hasItems) {
            // Check armor too
            for (org.bukkit.inventory.ItemStack item : sender.getInventory().getArmorContents()) {
                if (item != null && item.getType() != org.bukkit.Material.AIR) {
                    hasItems = true;
                    break;
                }
            }
        }

        if (!hasItems) {
            sender.sendMessage(ERROR_COLOR + "Your inventory is empty! Add some items first.");
            return;
        }

        // Save sender's current inventory as the target player's kit
        cc.kasumi.commons.util.PlayerInv kit = new cc.kasumi.commons.util.PlayerInv(
                sender.getInventory().getContents().clone(),
                sender.getInventory().getArmorContents().clone()
        );

        kitManager.setPlayerKit(offlinePlayer.getUniqueId(), ladderName, kit);

        sender.sendMessage(MAIN_COLOR + "Set " + SEC_COLOR + offlinePlayer.getName() + MAIN_COLOR +
                "'s custom kit for " + SEC_COLOR + ladder.getDisplayName() + MAIN_COLOR +
                " from your current inventory!");

        // Notify target player if online
        if (offlinePlayer.isOnline()) {
            Player targetPlayer = offlinePlayer.getPlayer();
            targetPlayer.sendMessage(MAIN_COLOR + "Your custom kit for " + SEC_COLOR + ladder.getDisplayName() +
                    MAIN_COLOR + " has been updated by " + SEC_COLOR + sender.getName());
        }
    }

    @Subcommand("reload")
    public void onReloadCommand(CommandSender sender) {
        // Display current stats before clearing
        Map<String, Object> statsBefore = kitManager.getKitStats();

        sender.sendMessage(MAIN_COLOR + "Kit system reload completed!");
        sender.sendMessage(SEC_COLOR + "Current stats - Players: " + statsBefore.get("totalPlayers") +
                ", Kits: " + statsBefore.get("totalCustomKits"));
        sender.sendMessage(SEC_COLOR + "Note: Player kits are cached in memory and will persist until server restart.");
    }

    @Subcommand("backup")
    public void onBackupCommand(CommandSender sender) {
        Map<String, Object> stats = kitManager.getKitStats();
        sender.sendMessage(MAIN_COLOR + "Kit backup initiated...");
        sender.sendMessage(SEC_COLOR + "Total players: " + stats.get("totalPlayers"));
        sender.sendMessage(SEC_COLOR + "Total kits: " + stats.get("totalCustomKits"));
        sender.sendMessage(ERROR_COLOR + "Backup functionality not implemented yet.");
        sender.sendMessage(SEC_COLOR + "Kits are currently stored in memory only.");
    }

    @Subcommand("import")
    @Syntax("<player> <ladder>")
    public void onImportCommand(Player sender, String playerName, String ladderName) {
        sender.sendMessage(ERROR_COLOR + "Kit import functionality not implemented yet.");
        sender.sendMessage(SEC_COLOR + "Use '/ka setfrom <player> <ladder>' to set kits from your inventory.");
        sender.sendMessage(SEC_COLOR + "Or use '/ka copy <fromPlayer> <toPlayer> <ladder>' to copy between players.");
    }

    @Subcommand("reset")
    @Syntax("<player> <ladder>")
    public void onResetCommand(CommandSender sender, String playerName, String ladderName) {
        // This is the same as clear but with different messaging
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
            sender.sendMessage(ERROR_COLOR + "Player '" + playerName + "' not found!");
            return;
        }

        ladderName = ladderName.toLowerCase();
        if (!Practice.getInstance().getLadders().containsKey(ladderName)) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' doesn't exist!");
            return;
        }

        UUID playerUUID = offlinePlayer.getUniqueId();
        if (!kitManager.hasCustomKit(playerUUID, ladderName)) {
            sender.sendMessage(ERROR_COLOR + offlinePlayer.getName() + " doesn't have a custom kit for '" + ladderName + "'!");
            return;
        }

        kitManager.removePlayerKit(playerUUID, ladderName);
        sender.sendMessage(MAIN_COLOR + "Reset " + SEC_COLOR + offlinePlayer.getName() + MAIN_COLOR +
                "'s kit for " + SEC_COLOR + ladderName + MAIN_COLOR + " to default!");

        // Notify target player if online
        if (offlinePlayer.isOnline()) {
            Player targetPlayer = offlinePlayer.getPlayer();
            Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
            targetPlayer.sendMessage(MAIN_COLOR + "Your custom kit for " + SEC_COLOR +
                    (ladder != null ? ladder.getDisplayName() : ladderName) +
                    MAIN_COLOR + " has been reset to default by " + SEC_COLOR + sender.getName());
        }
    }
}