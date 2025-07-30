package cc.kasumi.practice.game.queue;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class QueuePlayer {

    private final UUID uuid;

    private int rating;
    private int range = 25;

    public QueuePlayer(UUID uuid) {
        this.uuid = uuid;
        this.rating = 1000; // Default rating for unranked matches
    }

    public QueuePlayer(UUID uuid, int rating) {
        this.uuid = uuid;
        this.rating = rating;
    }

    public boolean isInRange(int otherRating) {
        return otherRating >= (this.rating - this.range) && otherRating <= (this.rating + this.range);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}