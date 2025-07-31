package cc.kasumi.practice.util;

import cc.kasumi.commons.util.CC;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.nametag.NametagContent;
import cc.kasumi.practice.nametag.NametagManager;
import cc.kasumi.practice.nametag.PlayerNametag;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class NametagUtil {

    /**
     * Get health bar for nametag display
     */

    /**
     * Get food bar for nametag display
     */
    public static String getFoodBar(Player player) {
        int food = player.getFoodLevel();

        ChatColor color;
        if (food > 14) {
            color = ChatColor.GREEN;
        } else if (food > 6) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        return color + "\uD83C\uDF57" + MAIN_COLOR + (food / 2);
    }

    /**
     * Get ELO display for nametag
     */
    public static String getEloDisplay(PracticePlayer practicePlayer) {
        int elo = practicePlayer.getElo().getRating();

        ChatColor color;
        if (elo >= 1400) {
            color = ChatColor.GOLD;
        } else if (elo >= 1200) {
            color = ChatColor.GREEN;
        } else if (elo >= 1000) {
            color = ChatColor.YELLOW;
        } else {
            color = ChatColor.RED;
        }

        return SEC_COLOR + "[" + color + elo + SEC_COLOR + "]";
    }

    /**
     * Get ping display for nametag
     */
    public static String getPingDisplay(Player player) {
        try {
            // Try to get ping via reflection
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            int ping = (int) handle.getClass().getField("ping").get(handle);

            ChatColor color;
            if (ping <= 50) {
                color = ChatColor.GREEN;
            } else if (ping <= 100) {
                color = ChatColor.YELLOW;
            } else if (ping <= 200) {
                color = ChatColor.GOLD;
            } else {
                color = ChatColor.RED;
            }

            return color + "" + ping + "ms";
        } catch (Exception e) {
            return ChatColor.GRAY + "?ms";
        }
    }

    /**
     * Create match nametag content
     */
    public static NametagContent createMatchContent(Player player) {
        String prefix = getPingDisplay(player) + " ";
        return new NametagContent(prefix,"").truncate();
    }

    /**
     * Create lobby nametag content
     */
    public static NametagContent createLobbyContent(PracticePlayer practicePlayer) {
        String prefix = getEloDisplay(practicePlayer) + " ";
        return NametagContent.withPrefix(prefix).truncate();
    }

    /**
     * Create spectator nametag content
     */
    public static NametagContent createSpectatorContent() {
        String prefix = SEC_COLOR + "[" + MAIN_COLOR + "Spec" + SEC_COLOR + "] ";
        return NametagContent.withPrefix(prefix).truncate();
    }

    /**
     * Create queue nametag content
     */
    public static NametagContent createQueueContent() {
        String prefix = SEC_COLOR + "[" + MAIN_COLOR + "Queue" + SEC_COLOR + "] ";
        return NametagContent.withPrefix(prefix).truncate();
    }

    /**
     * Force update all nametags for all players
     */
    public static void refreshAllNametags() {
        NametagManager nametagManager = Practice.getInstance().getNametagManager();
        nametagManager.getNametags().forEach((uuid, nametag) -> {
            nametagManager.updateNametag(nametag);
        });
    }

    /**
     * Clear all nametags for a specific player (useful for debugging)
     */
    public static void clearAllNametagsFor(Player viewer) {
        NametagManager nametagManager = Practice.getInstance().getNametagManager();
        nametagManager.getNametags().forEach((uuid, nametag) -> {
            if (!nametag.getPlayer().equals(viewer)) {
                nametag.hideFor(viewer);
            }
        });
    }

    /**
     * Show custom admin nametag
     */
    public static NametagContent createAdminContent() {
        String prefix = CC.GRAY + "[" + CC.DARK_RED + "Admin" + ChatColor.GRAY + "] " + CC.DARK_RED;
        return NametagContent.withPrefix(prefix).truncate();
    }

    /**
     * Get builder status nametag
     */
    public static NametagContent createBuilderContent() {
        String prefix = ChatColor.BLUE + "[" + ChatColor.AQUA + "Builder" + ChatColor.BLUE + "] ";
        return NametagContent.withPrefix(prefix).truncate();
    }
}