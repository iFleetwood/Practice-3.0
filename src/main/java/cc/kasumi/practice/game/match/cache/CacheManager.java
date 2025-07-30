package cc.kasumi.practice.game.match.cache;

import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
public class CacheManager {

    private final Map<UUID, CachedInventory> cachedInventories = new HashMap<>();

    public void cachePlayerInventory(Player player, CachedInventoryType type, boolean playerDead) {
        cachedInventories.put(player.getUniqueId(), new CachedInventory(player, type, playerDead));
    }
}
