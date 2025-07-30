package cc.kasumi.practice.game.duel;

import cc.kasumi.practice.game.ladder.Ladder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class DuelRequest {

    private final UUID sender;
    private Ladder ladder;

    private long timestamp;

    public DuelRequest(UUID sender, Ladder ladder) {
        this.sender = sender;
        this.ladder = ladder;
        this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - this.timestamp >= 30_000;
    }
}
