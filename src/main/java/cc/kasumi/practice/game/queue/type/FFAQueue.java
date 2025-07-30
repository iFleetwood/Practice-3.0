package cc.kasumi.practice.game.queue.type;

import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.QueuePlayer;
import cc.kasumi.practice.game.queue.QueueType;

import java.util.ArrayList;
import java.util.List;

public class FFAQueue extends Queue {

    private static final int MIN_PLAYERS = 3;
    private static final int MAX_PLAYERS = 10;

    public FFAQueue(String name, Ladder ladder, boolean ranked) {
        super(name, QueueType.FFA, ladder, ranked);
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

        // Take up to MAX_PLAYERS for the match
        int playersToTake = Math.min(getPlayerCount(), MAX_PLAYERS);

        for (int i = 0; i < playersToTake; i++) {
            playersForMatch.add(players.get(i));
        }

        // Remove these players from the queue
        for (QueuePlayer player : playersForMatch) {
            players.remove(player);
        }

        return playersForMatch;
    }

    @Override
    public String getQueueStatus() {
        return String.format("%d/%d players", getPlayerCount(), getMaxPlayers());
    }

    @Override
    public String getReadyStatusMessage() {
        if (isReadyForMatch()) {
            if (isFull()) {
                return "§aQueue full - Starting match!";
            } else {
                return "§aReady to start!";
            }
        } else {
            int needed = getMinPlayers() - getPlayerCount();
            return String.format("§eWaiting for %d more player%s...", needed, needed == 1 ? "" : "s");
        }
    }
}