package cc.kasumi.practice.util;

import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.match.team.MatchTeam;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SpectatorUtil {

    /**
     * Give spectator items to a player
     */
    public static void giveSpectatorItems(Player spectator) {
        spectator.getInventory().clear();

        ItemStack[] spectatorItems = {
                new ItemBuilder(Material.COMPASS)
                        .name(CC.GOLD + "Teleport to Players")
                        .lore(CC.YELLOW + "Right click to open teleport menu")
                        .build(),
                null,
                null,
                null,
                new ItemBuilder(Material.REDSTONE)
                        .name(CC.RED + "Stop Spectating")
                        .lore(CC.YELLOW + "Right click to return to lobby")
                        .build(),
                null,
                null,
                null,
                null
        };

        spectator.getInventory().setContents(spectatorItems);
    }

    /**
     * Remove spectator items and give lobby items
     */
    public static void removeSpectatorItems(Player player) {
        player.getInventory().setContents(GameUtil.getLobbyContents());
    }

    /**
     * Get list of players that spectator can teleport to
     */
    public static List<Player> getTeleportablePlayers(Player spectator) {
        List<Player> players = new ArrayList<>();

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(spectator.getUniqueId());
        if (practicePlayer == null || !practicePlayer.isSpectating()) {
            return players;
        }

        Match spectatingMatch = practicePlayer.getSpectatingMatch();
        if (spectatingMatch == null) {
            return players;
        }

        // Add all match players
        for (MatchTeam team : spectatingMatch.getTeams()) {
            players.addAll(team.getBukkitPlayers());
        }

        return players;
    }

    /**
     * Teleport spectator to a specific player
     */
    public static void teleportToPlayer(Player spectator, Player target) {
        if (target == null || !target.isOnline()) {
            spectator.sendMessage(CC.RED + "That player is no longer online!");
            return;
        }

        // Teleport slightly above and behind the target
        spectator.teleport(target.getLocation().add(0, 2, 0));
        spectator.sendMessage(CC.GREEN + "Teleported to " + CC.YELLOW + target.getName());
    }

    /**
     * Check if a player can spectate a specific match
     */
    public static boolean canSpectate(Player spectator, Match match) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(spectator.getUniqueId());

        if (practicePlayer == null) {
            return false;
        }

        // Can't spectate if already in a match
        if (practicePlayer.isInMatch()) {
            return false;
        }

        // Can't spectate if match is null or not ongoing
        if (match == null || match.getMatchState() != cc.kasumi.practice.game.match.MatchState.ONGOING) {
            return false;
        }

        return true;
    }

    /**
     * Get spectator information string for a match
     */
    public static String getSpectatorInfo(Match match) {
        int spectatorCount = match.getSpectators().size();

        if (spectatorCount == 0) {
            return CC.GRAY + "No spectators";
        } else if (spectatorCount == 1) {
            return CC.YELLOW + "1 spectator";
        } else {
            return CC.YELLOW + spectatorCount + " spectators";
        }
    }
}