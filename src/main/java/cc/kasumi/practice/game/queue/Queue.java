package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.game.ladder.Ladder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Getter
public abstract class Queue {

    protected final List<QueuePlayer> players = new LinkedList<>();
    protected final String name;
    protected final QueueType type;
    protected final Ladder ladder;
    protected final boolean ranked;

    protected Queue(String name, QueueType type, Ladder ladder, boolean ranked) {
        this.name = name;
        this.type = type;
        this.ladder = ladder;
        this.ranked = ranked;
    }

    // Abstract methods that subclasses must implement
    public abstract boolean isReadyForMatch();
    public abstract int getMinPlayers();
    public abstract int getMaxPlayers();
    public abstract List<QueuePlayer> getPlayersForMatch();

    // Common methods that all queues share
    public void addPlayer(QueuePlayer player) {
        if (!isFull()) {
            players.add(player);
        }
    }

    public void removePlayerByUUID(UUID uuid) {
        players.removeIf(queuePlayer -> queuePlayer.getUuid().equals(uuid));
    }

    public boolean isFull() {
        return players.size() >= getMaxPlayers();
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public int getPlayerCount() {
        return players.size();
    }

    // Template method for getting queue status
    public String getQueueStatus() {
        return String.format("%d/%d players", getPlayerCount(), getMaxPlayers());
    }

    // Template method for getting ready status message
    public String getReadyStatusMessage() {
        if (isReadyForMatch()) {
            return "§aReady to start!";
        } else {
            int needed = getMinPlayers() - getPlayerCount();
            return String.format("§eWaiting for %d more player%s...", needed, needed == 1 ? "" : "s");
        }
    }
}