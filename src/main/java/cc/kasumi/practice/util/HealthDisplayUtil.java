package cc.kasumi.practice.util;

import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class HealthDisplayUtil {

    /**
     * Update health display based on player state
     */
    public static void updateHealthDisplay(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (practicePlayer == null) {
            return;
        }

        // Show health below name only during matches
        if (practicePlayer.getPlayerState() == PlayerState.PLAYING) {
            enableHealthDisplay(player);
        } else {
            disableHealthDisplay(player);
        }
    }

    /**
     * Enable health display below names
     */
    private static void enableHealthDisplay(Player player) {
        Scoreboard scoreboard = player.getScoreboard();

        // Check if we already have a health objective
        Objective healthObjective = scoreboard.getObjective("belowNameHealth");

        if (healthObjective == null) {
            // Create new health objective
            healthObjective = scoreboard.registerNewObjective("belowNameHealth", "health");
            healthObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
            healthObjective.setDisplayName("❤"); // Heart symbol
        }

        // Update health scores for all visible players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (player.canSee(onlinePlayer)) {
                int health = (int) Math.ceil(onlinePlayer.getHealth());
                healthObjective.getScore(onlinePlayer.getName()).setScore(health);
            }
        }
    }

    /**
     * Disable health display below names
     */
    private static void disableHealthDisplay(Player player) {
        Scoreboard scoreboard = player.getScoreboard();
        Objective healthObjective = scoreboard.getObjective("belowNameHealth");

        if (healthObjective != null) {
            healthObjective.unregister();
        }
    }

    /**
     * Update health score for a specific player
     */
    public static void updateHealthScore(Player target) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.canSee(target)) {
                Scoreboard scoreboard = onlinePlayer.getScoreboard();
                Objective healthObjective = scoreboard.getObjective("belowNameHealth");

                if (healthObjective != null) {
                    int health = (int) Math.ceil(target.getHealth());
                    healthObjective.getScore(target.getName()).setScore(health);
                }
            }
        }
    }

    /**
     * Force refresh all health displays
     */
    public static void refreshAllHealthDisplays() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateHealthDisplay(player);
        }
    }
}