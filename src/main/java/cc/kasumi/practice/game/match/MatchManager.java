package cc.kasumi.practice.game.match;

import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.SoloTeam;
import cc.kasumi.practice.game.match.type.TvTMatch;
import lombok.Getter;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

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
}
