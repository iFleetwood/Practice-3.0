package cc.kasumi.practice.scoreboard.type;

import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.scoreboard.ScoreboardProvider;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class ProviderResolver {

    private final Map<PlayerState, ScoreboardProvider> scoreboardProviders = new HashMap<>();

    public ProviderResolver() {
        scoreboardProviders.put(PlayerState.LOBBY, new LobbyScoreboardProvider());
        scoreboardProviders.put(PlayerState.QUEUEING, new LobbyScoreboardProvider());
        scoreboardProviders.put(PlayerState.EDITING, new LobbyScoreboardProvider());
        scoreboardProviders.put(PlayerState.SPECTATING, new LobbyScoreboardProvider());
        scoreboardProviders.put(PlayerState.PLAYING, new PlayingScoreboardProvider());
    }
}
