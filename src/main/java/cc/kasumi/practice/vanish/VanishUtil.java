package cc.kasumi.practice.vanish;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class VanishUtil {

    private static VanishManager getVanishManager() {
        return Practice.getInstance().getVanishManager();
    }

    /**
     * Show all players to each other (lobby mode)
     */
    public static void showAllPlayers(Player player) {
        VanishManager vanishManager = getVanishManager();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.showPlayer(onlinePlayer);
            onlinePlayer.showPlayer(player);

            // Update vanish manager
            vanishManager.addCanSee(player.getUniqueId(), onlinePlayer.getUniqueId());
            vanishManager.addCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
        }
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

            // Update vanish manager
            vanishManager.removeCanSee(player.getUniqueId(), onlinePlayer.getUniqueId());
            vanishManager.removeCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
        }
    }

    /**
     * Show only match players to each other
     */
    public static void showMatchPlayers(Match match) {
        VanishManager vanishManager = getVanishManager();
        Set<Player> matchPlayers = match.getBukkitPlayers();

        for (Player player : matchPlayers) {
            Set<UUID> canSeeUUIDs = matchPlayers.stream()
                    .filter(p -> p != player)
                    .map(Player::getUniqueId)
                    .collect(Collectors.toSet());

            vanishManager.setCanSee(player.getUniqueId(), canSeeUUIDs);

            // Actually show/hide players
            for (Player otherPlayer : matchPlayers) {
                if (otherPlayer != player) {
                    player.showPlayer(otherPlayer);
                }
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
    }

    /**
     * Show specific player to all others
     */
    public static void showPlayerToAll(Player player) {
        VanishManager vanishManager = getVanishManager();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer != player) {
                onlinePlayer.showPlayer(player);
                vanishManager.addCanSee(onlinePlayer.getUniqueId(), player.getUniqueId());
            }
        }
    }

    /**
     * Set up vanish for spectators
     */
    public static void setupSpectatorVanish(Player spectator, Match match) {
        VanishManager vanishManager = getVanishManager();

        // Spectator can see all match players
        Set<UUID> canSeeUUIDs = match.getBukkitPlayers().stream()
                .map(Player::getUniqueId)
                .collect(Collectors.toSet());

        vanishManager.setCanSee(spectator.getUniqueId(), canSeeUUIDs);

        // Match players cannot see spectator
        for (Player matchPlayer : match.getBukkitPlayers()) {
            matchPlayer.hidePlayer(spectator);
            spectator.showPlayer(matchPlayer);
        }
    }

    /**
     * Smart vanish setup based on player state
     */
    public static void updatePlayerVanish(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (practicePlayer == null) {
            showAllPlayers(player);
            return;
        }

        switch (practicePlayer.getPlayerState()) {
            case LOBBY:
                showAllPlayers(player);
                break;

            case PLAYING:
                if (practicePlayer.getCurrentMatch() != null) {
                    showMatchPlayers(practicePlayer.getCurrentMatch());
                }
                break;

            case SPECTATING:
                if (practicePlayer.getSpectatingMatch() != null) {
                    setupSpectatorVanish(player, practicePlayer.getSpectatingMatch());
                }
                break;

            case QUEUEING:
            case EDITING:
                hideAllPlayers(player);
                break;
        }
    }

    /**
     * Check if two players should see each other
     */
    public static boolean shouldSeeEachOther(Player viewer, Player target) {
        return getVanishManager().shouldShowSource(target.getUniqueId(), viewer.getUniqueId());
    }
}