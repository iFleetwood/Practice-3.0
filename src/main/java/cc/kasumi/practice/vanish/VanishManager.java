package cc.kasumi.practice.vanish;

import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class VanishManager {

    private final Map<Integer, UUID> sources = new ConcurrentHashMap<>();
    private final Map<Location, UUID> particles = new ConcurrentHashMap<>();

    // Track vanished players for easier management
    private final Set<UUID> vanishedPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<UUID>> canSeeMap = new ConcurrentHashMap<>();

    /**
     * Register a projectile/item source for vanish tracking
     */
    public void registerSource(int entityId, UUID sourceUUID) {
        sources.put(entityId, sourceUUID);
    }

    /**
     * Remove a source when entity despawns/hits
     */
    public void unregisterSource(int entityId) {
        sources.remove(entityId);
    }

    /**
     * Register particle effect for vanish filtering
     */
    public void registerParticle(Location location, UUID sourceUUID) {
        particles.put(location, sourceUUID);
    }

    /**
     * Clean up old particle locations
     */
    public void cleanupParticles() {
        particles.clear();
    }

    /**
     * Check if player should see another player
     */
    public boolean canSee(UUID viewer, UUID target) {
        Set<UUID> viewerCanSee = canSeeMap.get(viewer);
        return viewerCanSee != null && viewerCanSee.contains(target);
    }

    /**
     * Set who a player can see (used by match system)
     */
    public void setCanSee(UUID viewer, Set<UUID> targets) {
        canSeeMap.put(viewer, new HashSet<>(targets));
    }

    /**
     * Add a player that another can see
     */
    public void addCanSee(UUID viewer, UUID target) {
        canSeeMap.computeIfAbsent(viewer, k -> ConcurrentHashMap.newKeySet()).add(target);
    }

    /**
     * Remove visibility between players
     */
    public void removeCanSee(UUID viewer, UUID target) {
        Set<UUID> viewerCanSee = canSeeMap.get(viewer);
        if (viewerCanSee != null) {
            viewerCanSee.remove(target);
        }
    }

    /**
     * Clear all visibility for a player (when they leave)
     */
    public void clearPlayer(UUID playerUUID) {
        canSeeMap.remove(playerUUID);
        // Remove player from others' can-see lists
        canSeeMap.values().forEach(set -> set.remove(playerUUID));
        vanishedPlayers.remove(playerUUID);
    }

    /**
     * Check if source player should be visible to viewer
     */
    public boolean shouldShowSource(UUID sourceUUID, UUID viewerUUID) {
        if (sourceUUID.equals(viewerUUID)) {
            return true; // Always see your own stuff
        }

        return canSee(viewerUUID, sourceUUID);
    }
}