package cc.kasumi.practice.kit;

import cc.kasumi.commons.util.BukkitStringUtil;
import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerKitManager {

    // Cache: PlayerUUID -> LadderName -> PlayerInv
    private final Map<UUID, Map<String, PlayerInv>> playerKits = new ConcurrentHashMap<>();

    /**
     * Get a player's custom kit for a ladder
     */
    public PlayerInv getPlayerKit(UUID playerUUID, String ladderName) {
        Map<String, PlayerInv> playerLadderKits = playerKits.get(playerUUID);
        if (playerLadderKits == null) {
            return null;
        }
        return playerLadderKits.get(ladderName.toLowerCase());
    }

    /**
     * Set a player's custom kit for a ladder
     */
    public void setPlayerKit(UUID playerUUID, String ladderName, PlayerInv kit) {
        playerKits.computeIfAbsent(playerUUID, k -> new ConcurrentHashMap<>())
                .put(ladderName.toLowerCase(), kit);
    }

    /**
     * Remove a player's custom kit for a ladder
     */
    public void removePlayerKit(UUID playerUUID, String ladderName) {
        Map<String, PlayerInv> playerLadderKits = playerKits.get(playerUUID);
        if (playerLadderKits != null) {
            playerLadderKits.remove(ladderName.toLowerCase());
            if (playerLadderKits.isEmpty()) {
                playerKits.remove(playerUUID);
            }
        }
    }

    /**
     * Check if player has a custom kit for a ladder
     */
    public boolean hasCustomKit(UUID playerUUID, String ladderName) {
        return getPlayerKit(playerUUID, ladderName) != null;
    }

    /**
     * Get the effective kit for a player (custom if exists, otherwise ladder default)
     */
    public PlayerInv getEffectiveKit(Player player, Ladder ladder) {
        PlayerInv customKit = getPlayerKit(player.getUniqueId(), ladder.getName());
        return customKit != null ? customKit : ladder.getDefaultKit();
    }

    /**
     * Save a player's current inventory as their custom kit
     */
    public void saveCurrentInventoryAsKit(Player player, Ladder ladder) {
        PlayerInv kit = new PlayerInv(
                player.getInventory().getContents().clone(),
                player.getInventory().getArmorContents().clone()
        );
        setPlayerKit(player.getUniqueId(), ladder.getName(), kit);
    }

    /**
     * Load player kit data from database/storage
     */
    public void loadPlayerKits(UUID playerUUID) {
        // TODO: Implement loading from MongoDB or file storage
        // This would integrate with your existing player data system
    }

    /**
     * Save player kit data to database/storage
     */
    public void savePlayerKits(UUID playerUUID) {
        // TODO: Implement saving to MongoDB or file storage
        Map<String, PlayerInv> playerLadderKits = playerKits.get(playerUUID);
        if (playerLadderKits == null || playerLadderKits.isEmpty()) {
            return;
        }

        // Example of how you might save to MongoDB:
        /*
        Map<String, String> serializedKits = new HashMap<>();
        for (Map.Entry<String, PlayerInv> entry : playerLadderKits.entrySet()) {
            serializedKits.put(entry.getKey(), BukkitStringUtil.playerInvToString(entry.getValue()));
        }
        // Save serializedKits to database
        */
    }

    /**
     * Clear all kits for a player (when they leave)
     */
    public void clearPlayerKits(UUID playerUUID) {
        playerKits.remove(playerUUID);
    }

    /**
     * Get all ladder names that a player has custom kits for
     */
    public String[] getCustomKitLadders(UUID playerUUID) {
        Map<String, PlayerInv> playerLadderKits = playerKits.get(playerUUID);
        if (playerLadderKits == null) {
            return new String[0];
        }
        return playerLadderKits.keySet().toArray(new String[0]);
    }

    /**
     * Get statistics about custom kits
     */
    public Map<String, Object> getKitStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", playerKits.size());

        int totalKits = 0;
        Map<String, Integer> kitsPerLadder = new HashMap<>();

        for (Map<String, PlayerInv> playerLadderKits : playerKits.values()) {
            totalKits += playerLadderKits.size();

            for (String ladderName : playerLadderKits.keySet()) {
                kitsPerLadder.put(ladderName, kitsPerLadder.getOrDefault(ladderName, 0) + 1);
            }
        }

        stats.put("totalCustomKits", totalKits);
        stats.put("kitsPerLadder", kitsPerLadder);

        return stats;
    }
}