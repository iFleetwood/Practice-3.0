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
            int wins = practicePlayer.getLadderWins().getOrDefault(ladder, 0);
            int losses = practicePlayer.getLadderLosses().getOrDefault(ladder, 0);
            int totalMatches = practicePlayer.getTotalMatches(ladder);
            
            if (totalMatches > 0) {
                int rating = practicePlayer.getLadderElo(ladder).getRating();
                double winRate = practicePlayer.getWinRate(ladder);
                int winStreak = practicePlayer.getLadderWinStreaks().getOrDefault(ladder, 0);
                int bestStreak = practicePlayer.getLadderBestWinStreaks().getOrDefault(ladder, 0);
                
                player.sendMessage("  " + SEC_COLOR + ladder.getDisplayName() + ":");
                player.sendMessage("    " + MAIN_COLOR + "ELO: " + SEC_COLOR + rating);
                player.sendMessage("    " + MAIN_COLOR + "Record: " + SEC_COLOR + wins + "W/" + losses + "L (" + df.format(winRate) + "%)");
                player.sendMessage("    " + MAIN_COLOR + "Win Streak: " + SEC_COLOR + winStreak + " (Best: " + bestStreak + ")");
            }
        }
        
        player.sendMessage("");
        player.sendMessage(MAIN_COLOR + "═══════════════════════════════════════");
    }
}