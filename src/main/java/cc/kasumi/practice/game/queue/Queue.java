package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.game.ladder.Ladder;
import lombok.Getter;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

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
    
    /**
     * Get detailed queue information for ranked queues
     */
    public String getDetailedQueueInfo() {
        if (!ranked || players.isEmpty()) {
            return getQueueStatus();
        }
        
        StringBuilder info = new StringBuilder();
        info.append(getQueueStatus()).append("\n\n");
        info.append("§7Players in queue:\n");
        
        for (QueuePlayer queuePlayer : players) {
            Player player = queuePlayer.getPlayer();
            if (player != null) {
                info.append(String.format("§f%s §7- %d ELO (±%d) [%s]\n", 
                    player.getName(),
                    queuePlayer.getRating(),
                    queuePlayer.getRange(),
                    queuePlayer.getFormattedQueueTime()
                ));
            }
        }
        
        return info.toString();
    }
    
    /**
     * Get average queue time for all players
     */
    public long getAverageQueueTime() {
        if (players.isEmpty()) {
            return 0;
        }
        
        long totalTime = 0;
        int validPlayers = 0;
        
        for (QueuePlayer queuePlayer : players) {
            if (queuePlayer.getPlayer() != null) {
                totalTime += queuePlayer.getQueueTimeSeconds();
                validPlayers++;
            }
        }
        
        return validPlayers > 0 ? totalTime / validPlayers : 0;
    }
}