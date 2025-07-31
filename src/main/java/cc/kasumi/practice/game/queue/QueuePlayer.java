package cc.kasumi.practice.game.queue;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class QueuePlayer {

    private final UUID uuid;
    private final long queueStartTime;

    private int rating;
    private int baseRange = 25; // Starting range
    private int maxRange = 200; // Maximum range after long queue times
    private int lastNotifiedRange = 25; // Track last range we notified about

    public QueuePlayer(UUID uuid) {
        this.uuid = uuid;
        this.rating = 1000; // Default rating for unranked matches
        this.queueStartTime = System.currentTimeMillis();
    }

    public QueuePlayer(UUID uuid, int rating) {
        this.uuid = uuid;
        this.rating = rating;
        this.queueStartTime = System.currentTimeMillis();
    }

    /**
     * Get current search range based on time in queue
     */
    public int getCurrentRange() {
        long timeInQueue = System.currentTimeMillis() - queueStartTime;
        long secondsInQueue = timeInQueue / 1000;
        
        // Expand range every 30 seconds
        // 0-30s: ±25, 30-60s: ±50, 60-90s: ±75, etc.
        int rangeIncrements = (int) (secondsInQueue / 10);
        int expandedRange = baseRange + (rangeIncrements * 25);
        
        // Cap at maximum range
        return Math.min(expandedRange, maxRange);
    }

    /**
     * Check if the range has expanded since last notification
     */
    public boolean hasRangeExpanded() {
        int currentRange = getCurrentRange();
        if (currentRange > lastNotifiedRange) {
            lastNotifiedRange = currentRange;
            return true;
        }
        return false;
    }

    /**
     * Reset the notification tracking (used when player joins queue)
     */
    public void resetNotificationTracking() {
        lastNotifiedRange = getCurrentRange();
    }

    /**
     * Check if another player's rating is within current search range
     */
    public boolean isInRange(int otherRating) {
        int currentRange = getCurrentRange();
        return otherRating >= (this.rating - currentRange) && otherRating <= (this.rating + currentRange);
    }

    /**
     * Get how long this player has been in queue (in seconds)
     */
    public long getQueueTimeSeconds() {
        return (System.currentTimeMillis() - queueStartTime) / 1000;
    }

    /**
     * Get formatted queue time for display
     */
    public String getFormattedQueueTime() {
        long seconds = getQueueTimeSeconds();
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, remainingSeconds);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getRating() {
        return rating;
    }

    public int getRange() {
        return getCurrentRange();
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}