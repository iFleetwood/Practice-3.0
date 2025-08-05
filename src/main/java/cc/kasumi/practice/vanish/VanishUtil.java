package cc.kasumi.practice.vanish;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.nametag.NametagManager;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VanishUtil {

    private static VanishManager getVanishManager() {
        return Practice.getInstance().getVanishManager();
    }

    private static void updateNametags(Player player) {
        NametagManager nametagManager = Practice.getInstance().getNametagManager();
        nametagManager.updateNametagsFor(player);
    }

    /**
     * Show all players to each other (lobby mode)
     */
    public static void showAllPlayers(Player player) {
        VanishManager vanishManager = getVanishManager();

        Set<UUID> canSeeUUIDs = new HashSet<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                player.showPlayer(onlinePlayer);
                onlinePlayer.showPlayer(player);
                canSeeUUIDs.add(onlinePlayer.getUniqueId());

                // Also update the other player's can-see list
                vanishManager.addCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
            }
        }

        vanishManager.setCanSee(player.getUniqueId(), canSeeUUIDs);
        updateNametags(player);
    }

    /**
     * Hide all players from each other (match setup)
     */
    public static void hideAllPlayers(Player player) {
        VanishManager vanishManager = getVanishManager();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == player) continue;

            player.hidePlayer(onlinePlayer);
            onlinePlayer.hidePlayer(player);

            // Clear visibility completely
            vanishManager.removeCanSee(player.getUniqueId(), onlinePlayer.getUniqueId());
            vanishManager.removeCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
        }

        updateNametags(player);
    }

    /**
     * Show only match players to each other - FIXED VERSION
     */
    public static void showMatchPlayers(Match match) {
        VanishManager vanishManager = getVanishManager();
        Set<Player> matchPlayers = match.getBukkitPlayers();
        Set<UUID> matchPlayerUUIDs = matchPlayers.stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        for (Player player : matchPlayers) {
            // First, hide this player from ALL other players
            hidePlayerFromAll(player);

            // Then show only match players to this player and vice versa
            Set<UUID> canSeeUUIDs = new HashSet<>(matchPlayerUUIDs);
            canSeeUUIDs.remove(player.getUniqueId()); // Remove self

            vanishManager.setCanSee(player.getUniqueId(), canSeeUUIDs);

            // Actually show/hide players based on the visibility rules
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer == player) continue;

                if (matchPlayerUUIDs.contains(onlinePlayer.getUniqueId())) {
                    // Show match players to each other
                    player.showPlayer(onlinePlayer);
                    onlinePlayer.showPlayer(player);

                    // Ensure both players can see each other in vanish manager
                    vanishManager.addCanSee(player.getUniqueId(), onlinePlayer.getUniqueId());
                    vanishManager.addCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
                } else {
                    // Hide non-match players
                    player.hidePlayer(onlinePlayer);
                    onlinePlayer.hidePlayer(player);

                    // Remove visibility in vanish manager
                    vanishManager.removeCanSee(player.getUniqueId(), onlinePlayer.getUniqueId());
                    vanishManager.removeCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
                }
            }

            updateNametags(player);
        }

        // Also update spectators if any
        for (UUID spectatorUUID : match.getSpectators()) {
            Player spectator = Bukkit.getPlayer(spectatorUUID);
            if (spectator != null) {
                setupSpectatorVanish(spectator, match);
            }
        }
    }

    /**
     * Hide specific player from all others
     */
    public static void hidePlayerFromAll(Player player) {
        VanishManager vanishManager = getVanishManager();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.hidePlayer(player);
                vanishManager.removeCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
            }
        }

        // Clear this player's can-see list
        vanishManager.setCanSee(player.getUniqueId(), new HashSet<>());
    }

    /**
     * Show specific player to all others
     */
    public static void showPlayerToAll(Player player) {
        VanishManager vanishManager = getVanishManager();

        Set<UUID> canSeeUUIDs = new HashSet<>();
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.showPlayer(player);
                vanishManager.addCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
                canSeeUUIDs.add(onlinePlayer.getUniqueId());
            }
        }

        vanishManager.setCanSee(player.getUniqueId(), canSeeUUIDs);
    }

    /**
     * Set up vanish for spectators - FIXED VERSION
     */
    public static void setupSpectatorVanish(Player spectator, Match match) {
        VanishManager vanishManager = getVanishManager();

        // First hide spectator from everyone
        hidePlayerFromAll(spectator);

        // Spectator can see all match players, but match players cannot see spectator
        Set<UUID> canSeeUUIDs = match.getBukkitPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        vanishManager.setCanSee(spectator.getUniqueId(), canSeeUUIDs);

        // Apply visibility settings
        for (Player matchPlayer : match.getBukkitPlayers()) {
            matchPlayer.hidePlayer(spectator); // Match players can't see spectator
            spectator.showPlayer(matchPlayer);  // Spectator can see match players

            // Remove spectator from match player's can-see list
            vanishManager.removeCanSee(matchPlayer.getUniqueId(), spectator.getUniqueId());
        }

        // Spectators should also be able to see other spectators
        for (UUID otherSpectatorUUID : match.getSpectators()) {
            if (!otherSpectatorUUID.equals(spectator.getUniqueId())) {
                Player otherSpectator = Bukkit.getPlayer(otherSpectatorUUID);
                if (otherSpectator != null) {
                    spectator.showPlayer(otherSpectator);
                    otherSpectator.showPlayer(spectator);

                    vanishManager.addCanSee(spectator.getUniqueId(), otherSpectatorUUID);
                    vanishManager.addCanSee(otherSpectatorUUID, spectator.getUniqueId());
                }
            }
        }

        updateNametags(spectator);
    }

    /**
     * Smart vanish setup based on player state - ENHANCED VERSION
     */
    public static void updatePlayerVanish(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (practicePlayer == null) {
            showAllPlayers(player);
            return;
        }

        switch (practicePlayer.getPlayerState()) {
            case LOBBY:
            case QUEUEING:
                showAllPlayers(player);
                break;

            case PLAYING:
                Match currentMatch = practicePlayer.getCurrentMatch();
                if (currentMatch != null) {
                    // Use the fixed showMatchPlayers method
                    showMatchPlayers(currentMatch);
                } else {
                    // Fallback to lobby visibility if no match
                    showAllPlayers(player);
                }
                break;

            case SPECTATING:
                Match spectatingMatch = practicePlayer.getSpectatingMatch();
                if (spectatingMatch != null) {
                    setupSpectatorVanish(player, spectatingMatch);
                } else {
                    // Fallback to lobby visibility if no match to spectate
                    showAllPlayers(player);
                }
                break;

            case EDITING:
                hideAllPlayers(player);
                break;

            default:
                showAllPlayers(player);
                break;
        }
    }

    /**
     * Check if two players should see each other
     */
    public static boolean shouldSeeEachOther(Player viewer, Player target) {
        return getVanishManager().shouldShowSource(target.getUniqueId(), viewer.getUniqueId());
    }

    /**
     * Force refresh vanish for all online players - useful for debugging
     */
    public static void refreshAllVanish() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerVanish(player);
        }
    }

    /**
     * Clean up vanish data for a match when it ends
     */
    public static void cleanupMatchVanish(Match match) {
        Set<Player> matchPlayers = match.getBukkitPlayers();

        // Reset all match players to lobby visibility
        for (Player player : matchPlayers) {
            PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
            if (practicePlayer != null && !practicePlayer.isInMatch()) {
                showAllPlayers(player);
            }
        }

        // Handle spectators
        for (UUID spectatorUUID : match.getSpectators()) {
            Player spectator = Bukkit.getPlayer(spectatorUUID);
            if (spectator != null) {
                PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(spectatorUUID);
                if (practicePlayer != null && !practicePlayer.isSpectating()) {
                    showAllPlayers(spectator);
                }
            }
        }
    }
}