package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.game.ladder.Ladder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
public class Queue {

    private final List<QueuePlayer> players = new LinkedList<>();

    private final String name;
    private final QueueType type;
    private final Ladder ladder;
    private final boolean ranked;

    public Queue(String name, QueueType type, Ladder ladder, boolean ranked) {
        this.name = name;
        this.type = type;
        this.ladder = ladder;
        this.ranked = ranked;
    }

    public void removePlayerByUUID(UUID uuid) {
        players.removeIf(queuePlayer -> queuePlayer.getUuid().equals(uuid));
    }
}
