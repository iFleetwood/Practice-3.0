package cc.kasumi.practice.scoreboard;

import cc.kasumi.practice.player.PracticePlayer;

import java.util.List;

public interface ScoreboardProvider {

    List<String> getLines(PlayerScoreboard playerScoreboard, PracticePlayer practicePlayer);
}
