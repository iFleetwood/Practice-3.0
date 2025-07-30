package cc.kasumi.practice.game.match.player;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class MatchPlayer {

    private final UUID uuid;
    private final String name;

    @Setter
    private boolean dead = false;
    @Setter
    private boolean disconnected = false;

    public MatchPlayer(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }
}
