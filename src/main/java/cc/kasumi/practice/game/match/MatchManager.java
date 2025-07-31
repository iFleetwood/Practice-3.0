package cc.kasumi.practice.game.match;

import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.MatchTeam;
import cc.kasumi.practice.game.match.team.SoloTeam;
import cc.kasumi.practice.game.match.team.TeamImpl;
import cc.kasumi.practice.game.match.type.FFAMatch;
import cc.kasumi.practice.game.match.type.TvTMatch;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class MatchManager {

    private final Set<Match> matches = new HashSet<>();

    public void createSoloMatch(@NonNull Player playerA, @NonNull Player playerB, Ladder ladder, Arena arena, boolean ranked) {
        MatchPlayer matchPlayerA = new MatchPlayer(playerA.getUniqueId(), playerA.getName());
        MatchPlayer matchPlayerB = new MatchPlayer(playerB.getUniqueId(), playerB.getName());
        SoloTeam soloTeamA = new SoloTeam(matchPlayerA);
        SoloTeam soloTeamB = new SoloTeam(matchPlayerB);

        Match match = new TvTMatch(MatchType.SOLO, ladder, arena, ranked, soloTeamA, soloTeamB);
        matches.add(match);
    }

    public void createFFAMatch(@NonNull List<Player> players, Ladder ladder, Arena arena, boolean ranked) {
        if (players.size() < 1) {
            throw new IllegalArgumentException("FFA matches require at least 1 player");
        }

        List<MatchPlayer> matchPlayers = players.stream()
                .map(player -> new MatchPlayer(player.getUniqueId(), player.getName()))
                .collect(Collectors.toList());

        Match match = new FFAMatch(MatchType.FFA, ladder, arena, ranked, matchPlayers);
        matches.add(match);
    }

    public void createTeamMatch(@NonNull List<Player> teamA, @NonNull List<Player> teamB, Ladder ladder, Arena arena, boolean ranked) {
        if (teamA.isEmpty() || teamB.isEmpty()) {
            throw new IllegalArgumentException("Both teams must have at least one player");
        }
        
        if (teamA.size() != teamB.size()) {
            throw new IllegalArgumentException("Teams must have equal number of players");
        }

        // Convert players to MatchPlayers for team A
        List<MatchPlayer> matchPlayersA = teamA.stream()
                .map(player -> new MatchPlayer(player.getUniqueId(), player.getName()))
                .collect(Collectors.toList());

        // Convert players to MatchPlayers for team B  
        List<MatchPlayer> matchPlayersB = teamB.stream()
                .map(player -> new MatchPlayer(player.getUniqueId(), player.getName()))
                .collect(Collectors.toList());

        // Create team objects
        MatchTeam matchTeamA = new TeamImpl(matchPlayersA);
        MatchTeam matchTeamB = new TeamImpl(matchPlayersB);

        // Create the team match
        Match match = new TvTMatch(MatchType.TEAM, ladder, arena, ranked, matchTeamA, matchTeamB);
        matches.add(match);
    }
}