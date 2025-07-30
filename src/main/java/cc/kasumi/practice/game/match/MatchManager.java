package cc.kasumi.practice.game.match;

import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.SoloTeam;
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
        // This method would be implemented for team matches (2v2, 3v3, etc.)
        // For now, just throw an exception since it's not implemented
        throw new UnsupportedOperationException("Team matches are not yet implemented");
    }
}