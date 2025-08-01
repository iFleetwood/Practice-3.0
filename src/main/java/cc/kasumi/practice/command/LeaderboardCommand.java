package cc.kasumi.practice.command;

import cc.kasumi.commons.mongodb.MCollection;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("leaderboard|lb|top")
public class LeaderboardCommand extends BaseCommand {

    @Default
    public void onLeaderboardCommand(Player player, @Optional String ladderName) {
        Ladder targetLadder = null;
        
        // Find the ladder if specified
        if (ladderName != null) {
            for (Ladder ladder : Practice.getInstance().getLadders().values()) {
                if (ladder.getName().equalsIgnoreCase(ladderName) || 
                    ladder.getDisplayName().equalsIgnoreCase(ladderName)) {
                    targetLadder = ladder;
                    break;
                }
            }
            
            if (targetLadder == null) {
                player.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' not found!");
                player.sendMessage(MAIN_COLOR + "Available ladders: " + 
                    Practice.getInstance().getLadders().values().stream()
                        .map(Ladder::getDisplayName)
                        .collect(Collectors.joining(", ")));
                return;
            }
        } else {
            // Default to first ladder if none specified
            targetLadder = Practice.getInstance().getLadders().values().iterator().next();
        }

        showLeaderboard(player, targetLadder);
    }

    private void showLeaderboard(Player player, Ladder ladder) {
        // Get all player documents from MongoDB
        MCollection playersCollection = Practice.getInstance().getPlayersCollection();
        
        Bukkit.getScheduler().runTaskAsynchronously(Practice.getInstance(), () -> {
            try {
                List<Document> allPlayers = playersCollection.getCollection().find().into(new ArrayList<>());
                
                // Create leaderboard entries
                List<LeaderboardEntry> entries = new ArrayList<>();
                
                for (Document doc : allPlayers) {
                    String uuidString = doc.getString("uuid");
                    if (uuidString == null) continue;
                    
                    UUID uuid = UUID.fromString(uuidString);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    
                    int rating = 1000; // Default rating
                    
                    // Get ladder-specific rating
                    if (doc.containsKey("ladderRatings")) {
                        Document ladderRatings = doc.get("ladderRatings", Document.class);
                        if (ladderRatings.containsKey(ladder.getName())) {
                            rating = ladderRatings.getInteger(ladder.getName(), 1000);
                        }
                    }
                    
                    // Get wins/losses (prioritize ranked for leaderboard)
                    int rankedWins = 0;
                    int rankedLosses = 0;
                    int totalWins = 0;
                    int totalLosses = 0;
                    
                    if (doc.containsKey("ladderRankedWins")) {
                        Document ladderRankedWins = doc.get("ladderRankedWins", Document.class);
                        rankedWins = ladderRankedWins.getInteger(ladder.getName(), 0);
                    }
                    
                    if (doc.containsKey("ladderRankedLosses")) {
                        Document ladderRankedLosses = doc.get("ladderRankedLosses", Document.class);
                        rankedLosses = ladderRankedLosses.getInteger(ladder.getName(), 0);
                    }
                    
                    if (doc.containsKey("ladderWins")) {
                        Document ladderWins = doc.get("ladderWins", Document.class);
                        totalWins = ladderWins.getInteger(ladder.getName(), 0);
                    }
                    
                    if (doc.containsKey("ladderLosses")) {
                        Document ladderLosses = doc.get("ladderLosses", Document.class);
                        totalLosses = ladderLosses.getInteger(ladder.getName(), 0);
                    }
                    
                    // Only include players who have played ranked matches (for competitive leaderboard)
                    if (rankedWins + rankedLosses > 0) {
                        entries.add(new LeaderboardEntry(offlinePlayer.getName(), rating, rankedWins, rankedLosses, totalWins, totalLosses));
                    }
                }
                
                // Sort by rating (highest first)
                entries.sort((a, b) -> Integer.compare(b.rating, a.rating));
                
                // Display leaderboard on main thread
                Bukkit.getScheduler().runTask(Practice.getInstance(), () -> {
                    displayLeaderboard(player, ladder, entries);
                });
                
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(Practice.getInstance(), () -> {
                    player.sendMessage(ERROR_COLOR + "Failed to load leaderboard data!");
                });
                e.printStackTrace();
            }
        });
    }
    
    private void displayLeaderboard(Player player, Ladder ladder, List<LeaderboardEntry> entries) {
        player.sendMessage("");
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
        player.sendMessage(MAIN_COLOR + "Top Players - " + SEC_COLOR + ladder.getDisplayName());
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
        player.sendMessage("");
        
        if (entries.isEmpty()) {
            player.sendMessage(ERROR_COLOR + "No players have played on this ladder yet!");
        } else {
            int rank = 1;
            for (LeaderboardEntry entry : entries.subList(0, Math.min(10, entries.size()))) {
                String rankColor = getRankColor(rank);
                double rankedWinRate = entry.getRankedWinRate();
                double totalWinRate = entry.getTotalWinRate();
                
                player.sendMessage(String.format("%s#%d %s%s %s- %s%d ELO",
                    rankColor, rank, SEC_COLOR, entry.playerName, MAIN_COLOR, SEC_COLOR, entry.rating));
                player.sendMessage(String.format("     %sRanked: %s%dW/%dL (%.1f%%) %s| Total: %s%dW/%dL (%.1f%%)",
                    MAIN_COLOR, SEC_COLOR, entry.rankedWins, entry.rankedLosses, rankedWinRate,
                    MAIN_COLOR, SEC_COLOR, entry.totalWins, entry.totalLosses, totalWinRate));
                rank++;
            }
        }
        
        player.sendMessage("");
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
    }
    
    private String getRankColor(int rank) {
        return switch (rank) {
            case 1 -> "§6"; // Gold
            case 2 -> "§7"; // Silver
            case 3 -> "§c"; // Bronze
            default -> "§f"; // White
        };
    }
    
    private static class LeaderboardEntry {
        final String playerName;
        final int rating;
        final int rankedWins;
        final int rankedLosses;
        final int totalWins;
        final int totalLosses;
        
        LeaderboardEntry(String playerName, int rating, int rankedWins, int rankedLosses, int totalWins, int totalLosses) {
            this.playerName = playerName;
            this.rating = rating;
            this.rankedWins = rankedWins;
            this.rankedLosses = rankedLosses;
            this.totalWins = totalWins;
            this.totalLosses = totalLosses;
        }
        
        double getRankedWinRate() {
            int total = rankedWins + rankedLosses;
            if (total == 0) return 0.0;
            return (double) rankedWins / total * 100.0;
        }
        
        double getTotalWinRate() {
            int total = totalWins + totalLosses;
            if (total == 0) return 0.0;
            return (double) totalWins / total * 100.0;
        }
    }
}