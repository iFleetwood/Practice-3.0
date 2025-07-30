package cc.kasumi.practice.scoreboard.type;

import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.scoreboard.PlayerScoreboard;
import cc.kasumi.practice.scoreboard.ScoreboardProvider;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class LobbyScoreboardProvider implements ScoreboardProvider {

    @Override
    public List<String> getLines(PlayerScoreboard playerScoreboard, PracticePlayer practicePlayer) {
        List<String> lines = new ArrayList<>();

        lines.add(SCOREBOARD_SPLITTER);
        lines.add(MAIN_COLOR + "Players Online: " + SEC_COLOR + Bukkit.getOnlinePlayers().size());
        lines.add(SCOREBOARD_SPLITTER);

        return lines;
    }
}
