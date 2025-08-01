package cc.kasumi.practice.command;

import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.player.PracticePlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Date;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("stats|statistics")
public class StatsCommand extends BaseCommand {

    private final DecimalFormat df = new DecimalFormat("#.##");

    @Default
    public void onStatsCommand(Player player, @Optional OnlinePlayer targetOnlinePlayer) {
        Player target = (targetOnlinePlayer != null) ? targetOnlinePlayer.getPlayer() : player;
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(target.getUniqueId());

        if (practicePlayer == null) {
            player.sendMessage(ERROR_COLOR + "Player data not found!");
            return;
        }

        String targetName = target.getName();
        boolean isOwnStats = target.equals(player);

        player.sendMessage("");
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
        player.sendMessage(MAIN_COLOR + "Statistics for " + SEC_COLOR + targetName);
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
        player.sendMessage("");

        // Global statistics
        player.sendMessage(SEC_COLOR + "Global Statistics:");
        player.sendMessage("  " + MAIN_COLOR + "Total Kills: " + SEC_COLOR + practicePlayer.getTotalKills());
        player.sendMessage("  " + MAIN_COLOR + "Total Deaths: " + SEC_COLOR + practicePlayer.getTotalDeaths());
        player.sendMessage("  " + MAIN_COLOR + "K/D Ratio: " + SEC_COLOR + df.format(practicePlayer.getKDRatio()));
        
        if (isOwnStats) {
            player.sendMessage("  " + MAIN_COLOR + "First Joined: " + SEC_COLOR + new Date(practicePlayer.getFirstJoined()));
            player.sendMessage("  " + MAIN_COLOR + "Last Seen: " + SEC_COLOR + new Date(practicePlayer.getLastSeen()));
        }
        
        player.sendMessage("");

        // Per-ladder statistics
        player.sendMessage(SEC_COLOR + "Ladder Statistics:");
        
        for (Ladder ladder : cc.kasumi.practice.Practice.getInstance().getLadders().values()) {
            int totalWins = practicePlayer.getLadderWins().getOrDefault(ladder, 0);
            int totalLosses = practicePlayer.getLadderLosses().getOrDefault(ladder, 0);
            int totalMatches = practicePlayer.getTotalMatches(ladder);
            
            if (totalMatches > 0) {
                int rating = practicePlayer.getLadderElo(ladder).getRating();
                double overallWinRate = practicePlayer.getWinRate(ladder);
                int winStreak = practicePlayer.getLadderWinStreaks().getOrDefault(ladder, 0);
                int bestStreak = practicePlayer.getLadderBestWinStreaks().getOrDefault(ladder, 0);
                
                // Ranked stats
                int rankedWins = practicePlayer.getLadderRankedWins().getOrDefault(ladder, 0);
                int rankedLosses = practicePlayer.getLadderRankedLosses().getOrDefault(ladder, 0);
                int rankedMatches = practicePlayer.getTotalRankedMatches(ladder);
                double rankedWinRate = practicePlayer.getRankedWinRate(ladder);
                int rankedWinStreak = practicePlayer.getLadderRankedWinStreaks().getOrDefault(ladder, 0);
                int bestRankedStreak = practicePlayer.getLadderBestRankedWinStreaks().getOrDefault(ladder, 0);
                
                // Unranked stats
                int unrankedWins = practicePlayer.getLadderUnrankedWins().getOrDefault(ladder, 0);
                int unrankedLosses = practicePlayer.getLadderUnrankedLosses().getOrDefault(ladder, 0);
                int unrankedMatches = practicePlayer.getTotalUnrankedMatches(ladder);
                double unrankedWinRate = practicePlayer.getUnrankedWinRate(ladder);
                
                player.sendMessage("  " + SEC_COLOR + ladder.getDisplayName() + ":");
                player.sendMessage("    " + MAIN_COLOR + "ELO: " + SEC_COLOR + rating);
                player.sendMessage("    " + MAIN_COLOR + "Overall: " + SEC_COLOR + totalWins + "W/" + totalLosses + "L (" + df.format(overallWinRate) + "%)");
                player.sendMessage("    " + MAIN_COLOR + "Win Streak: " + SEC_COLOR + winStreak + " (Best: " + bestStreak + ")");
                
                if (rankedMatches > 0) {
                    player.sendMessage("    " + MAIN_COLOR + "Ranked: " + SEC_COLOR + rankedWins + "W/" + rankedLosses + "L (" + df.format(rankedWinRate) + "%)");
                    player.sendMessage("    " + MAIN_COLOR + "Ranked Streak: " + SEC_COLOR + rankedWinStreak + " (Best: " + bestRankedStreak + ")");
                }
                
                if (unrankedMatches > 0) {
                    player.sendMessage("    " + MAIN_COLOR + "Unranked: " + SEC_COLOR + unrankedWins + "W/" + unrankedLosses + "L (" + df.format(unrankedWinRate) + "%)");
                }
            }
        }
        
        player.sendMessage("");
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
    }
}