package cc.kasumi.practice.scoreboard.type;

import cc.kasumi.commons.util.TimeUtil;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.match.MatchState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.scoreboard.PlayerScoreboard;
import cc.kasumi.practice.scoreboard.ScoreboardProvider;

import java.util.ArrayList;
import java.util.List;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class PlayingScoreboardProvider implements ScoreboardProvider {

    @Override
    public List<String> getLines(PlayerScoreboard playerScoreboard, PracticePlayer practicePlayer) {
        List<String> lines = new ArrayList<>();
        Match match = practicePlayer.getCurrentMatch();


        lines.add(SCOREBOARD_SPLITTER);

        if (match.getMatchState() == MatchState.ONGOING) {
            lines.add(MAIN_COLOR + "Match Time: " + SEC_COLOR + TimeUtil.getTimeFormatted(System.currentTimeMillis() - match.getTimestamp()));
        }

        lines.add(SCOREBOARD_SPLITTER);

        return lines;
    }
}
