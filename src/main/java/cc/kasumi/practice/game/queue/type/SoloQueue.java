package cc.kasumi.practice.game.queue.type;

import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.QueuePlayer;
import cc.kasumi.practice.game.queue.QueueType;

import java.util.ArrayList;
import java.util.List;

public class SoloQueue extends Queue {

    private static final int MIN_PLAYERS = 2;
    private static final int MAX_PLAYERS = 100; // Reasonable limit for solo queue

    public SoloQueue(String name, Ladder ladder, boolean ranked) {
        super(name, QueueType.SOLO, ladder, ranked);
    }

    @Override
    public boolean isReadyForMatch() {
        return getPlayerCount() >= MIN_PLAYERS;
    }

    @Override
    public int getMinPlayers() {
        return MIN_PLAYERS;
    }

    @Override
    public int getMaxPlayers() {
        return MAX_PLAYERS;
    }

    @Override
    public List<QueuePlayer> getPlayersForMatch() {
        List<QueuePlayer> playersForMatch = new ArrayList<>();

        if (!isReadyForMatch()) {
            return playersForMatch;
        }

        // For solo queue, we just need 2 players
        // The QueueThread will handle the matching logic
        return new ArrayList<>(players);
    }

    @Override
    public String getQueueStatus() {
        return String.format("%d players in queue", getPlayerCount());
    }
}