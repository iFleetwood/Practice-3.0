package cc.kasumi.practice.game.match.type;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.match.MatchState;
import cc.kasumi.practice.game.match.MatchType;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.MatchTeam;
import com.comphenix.protocol.wrappers.Pair;
import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;
import static cc.kasumi.practice.PracticeConfiguration.SEC_COLOR;

@Getter
public class TvTMatch extends Match {

    private final MatchTeam matchTeamA;
    private final MatchTeam matchTeamB;

    public TvTMatch(MatchType matchType, Ladder ladder, Arena arena, boolean ranked, MatchTeam matchTeamA, MatchTeam matchTeamB) {
        super(matchType, ladder, arena, ranked);

        this.matchTeamA = matchTeamA;
        this.matchTeamB = matchTeamB;

        // Now that teams are set, we can setup the match
        setupMatch();
    }

    @Override
    public Set<MatchTeam> getTeams() {
        Set<MatchTeam> teams = new HashSet<>();
        teams.add(matchTeamA);
        teams.add(matchTeamB);
        return teams;
    }

    @Override
    public void setupMatch() {
        List<Player> teamAPlayers = matchTeamA.getBukkitPlayers();
        List<Player> teamBPlayers = matchTeamB.getBukkitPlayers();

        // Setup players at their respective spawn locations
        for (Player player : teamAPlayers) {
            setupPlayer(player, arena.getSpawnLocationA());
        }

        for (Player player : teamBPlayers) {
            setupPlayer(player, arena.getSpawnLocationB());
        }

        // Show all players to each other
        showPlayersToEachOther(getBukkitPlayers());

        countdown();
    }

    @Override
    public String getMatchStartMessage() {
        return MAIN_COLOR + "The match has started!";
    }

    @Override
    public String getCountdownMessage(int seconds) {
        String secondText = seconds == 1 ? " second..." : " seconds...";
        return SEC_COLOR + "The match is starting in " + MAIN_COLOR + seconds + SEC_COLOR + secondText;
    }

    @Override
    public void handleMatchEnd() {
        List<MatchTeam> aliveTeams = getAliveTeams();

        if (aliveTeams.size() == 1) {
            // We have a winner
            MatchTeam winner = aliveTeams.get(0);
            MatchTeam loser = getOtherTeam(winner);

            cacheInventoriesForAlivePlayers(winner.getPlayers());

            new BukkitRunnable() {
                @Override
                public void run() {
                    endMatch(winner, loser);
                }
            }.runTaskLater(Practice.getInstance(), 2 * 20L);
        } else {
            // Draw case
            handleDraw();
        }
    }

    private void endMatch(MatchTeam winner, MatchTeam loser) {
        matchState = MatchState.ENDED;

        Pair<String, TextComponent> inventories = createAfterMatchMessage(winner.getPlayers(),
                getAllMatchPlayers());

        // Handle winners
        for (MatchPlayer matchPlayerWinner : winner.getPlayers()) {
            if (matchPlayerWinner.isDisconnected()) continue;

            Player playerWinner = matchPlayerWinner.getPlayer();
            if (playerWinner == null) continue;

            endMatchForPlayer(playerWinner, true);
            playerWinner.sendMessage(inventories.getFirst());
            playerWinner.spigot().sendMessage(inventories.getSecond());
        }

        // Handle losers
        for (MatchPlayer matchPlayerLoser : loser.getPlayers()) {
            if (matchPlayerLoser.isDisconnected()) continue;

            Player playerLoser = matchPlayerLoser.getPlayer();
            if (playerLoser == null) continue;

            endMatchForPlayer(playerLoser, false);
            playerLoser.sendMessage(inventories.getFirst());
            playerLoser.spigot().sendMessage(inventories.getSecond());
        }

        finishMatch();
    }

    private void handleDraw() {
        matchState = MatchState.ENDED;
        sendMessage(MAIN_COLOR + "The match ended in a draw!");

        List<MatchPlayer> allPlayers = getAllMatchPlayers();
        Pair<String, TextComponent> inventories = createAfterMatchMessage(allPlayers, allPlayers);

        for (MatchPlayer matchPlayer : allPlayers) {
            if (matchPlayer.isDisconnected()) continue;

            Player player = matchPlayer.getPlayer();
            if (player == null) continue;

            endMatchForPlayer(player, false);
            player.sendMessage(inventories.getFirst());
            player.spigot().sendMessage(inventories.getSecond());
        }

        finishMatch();
    }

    @Override
    protected String getEndMessage(Player player, boolean winner) {
        if (winner) {
            return MAIN_COLOR + "Congratulations you won!";
        } else {
            return MAIN_COLOR + "You lost!";
        }
    }

    private MatchTeam getOtherTeam(MatchTeam team) {
        return team == matchTeamA ? matchTeamB : matchTeamA;
    }

    private List<MatchPlayer> getAllMatchPlayers() {
        List<MatchPlayer> allPlayers = new ArrayList<>();
        allPlayers.addAll(matchTeamA.getPlayers());
        allPlayers.addAll(matchTeamB.getPlayers());
        return allPlayers;
    }
}