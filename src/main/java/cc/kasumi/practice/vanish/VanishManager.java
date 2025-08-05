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

    // Track which match each player is in for better isolation
    private final Map<UUID, UUID> playerToMatchMap = new ConcurrentHashMap<>();

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
        if (targets == null || targets.isEmpty()) {
            canSeeMap.put(viewer, new HashSet<>());
        } else {
            canSeeMap.put(viewer, new HashSet<>(targets));
        }
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
        playerToMatchMap.remove(playerUUID);
    }

    /**
     * Register a player as being in a specific match
     */
    public void setPlayerMatch(UUID playerUUID, UUID matchUUID) {
        if (matchUUID == null) {
            playerToMatchMap.remove(playerUUID);
        } else {
            playerToMatchMap.put(playerUUID, matchUUID);
        }
    }

    /**
     * Get the match UUID for a player
     */
    public UUID getPlayerMatch(UUID playerUUID) {
        return playerToMatchMap.get(playerUUID);
    }

    /**
     * Check if two players are in the same match
     */
    public boolean areInSameMatch(UUID player1UUID, UUID player2UUID) {
        UUID match1 = playerToMatchMap.get(player1UUID);
        UUID match2 = playerToMatchMap.get(player2UUID);

        return match1 != null && match2 != null && match1.equals(match2);
    }

    /**
     * Get all players in the same match as the given player
     */
    public Set<UUID> getPlayersInSameMatch(UUID playerUUID) {
        UUID matchUUID = playerToMatchMap.get(playerUUID);
        if (matchUUID == null) {
            return new HashSet<>();
        }

        Set<UUID> matchPlayers = new HashSet<>();
        for (Map.Entry<UUID, UUID> entry : playerToMatchMap.entrySet()) {
            if (matchUUID.equals(entry.getValue())) {
                matchPlayers.add(entry.getKey());
            }
        }
        return matchPlayers;
    }

    /**
     * Check if source player should be visible to viewer - ENHANCED VERSION
     */
    public boolean shouldShowSource(UUID sourceUUID, UUID viewerUUID) {
        if (sourceUUID.equals(viewerUUID)) {
            return true; // Always see your own stuff
        }

        // First check the explicit can-see mapping
        boolean explicitCanSee = canSee(viewerUUID, sourceUUID);

        // For additional safety, also check if they're in the same match
        boolean sameMatch = areInSameMatch(viewerUUID, sourceUUID);

        // They should see each other if they're explicitly allowed AND (in same match OR both not in matches)
        UUID viewerMatch = getPlayerMatch(viewerUUID);
        UUID sourceMatch = getPlayerMatch(sourceUUID);

        if (viewerMatch == null && sourceMatch == null) {
            // Both in lobby, use explicit can-see
            return explicitCanSee;
        } else if (viewerMatch != null && sourceMatch != null) {
            // Both in matches, they should only see if same match AND explicitly allowed
            return sameMatch && explicitCanSee;
        } else {
            // One in match, one not - they shouldn't see each other unless explicitly allowed
            return explicitCanSee;
        }
    }

    /**
     * Debug method to get visibility state
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("sourcesCount", sources.size());
        debug.put("particlesCount", particles.size());
        debug.put("vanishedPlayersCount", vanishedPlayers.size());
        debug.put("canSeeMapSize", canSeeMap.size());
        debug.put("playerToMatchMapSize", playerToMatchMap.size());
        return debug;
    }
}