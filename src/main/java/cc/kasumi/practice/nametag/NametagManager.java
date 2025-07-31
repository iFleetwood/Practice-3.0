package cc.kasumi.practice.nametag;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.nametag.type.ProviderResolver;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class NametagManager {

    private final Map<UUID, PlayerNametag> nametags = new ConcurrentHashMap<>();
    private final ProviderResolver providerResolver;
    private final BukkitTask bukkitTask;

    public NametagManager(Practice plugin) {
        Bukkit.getPluginManager().registerEvents(new NametagListener(), plugin);
        this.providerResolver = new ProviderResolver();
        this.bukkitTask = new NametagRunnable(this).runTaskTimerAsynchronously(plugin, 20, 20);
    }

    public void updateNametag(PlayerNametag playerNametag) {
        Player player = playerNametag.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (practicePlayer == null) {
            return;
        }

        // Get nametag content from provider
        NametagContent content = providerResolver.getNametagProviders()
                .get(practicePlayer.getPlayerState())
                .getContent(playerNametag, practicePlayer);

        // Update nametag for all viewers
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) continue;

            // Check if viewer should see this player's nametag
            if (shouldShowNametag(viewer, player)) {
                playerNametag.updateFor(viewer, content);
            } else {
                playerNametag.hideFor(viewer);
            }
        }

        // Update health display for this player
        cc.kasumi.practice.util.HealthDisplayUtil.updateHealthDisplay(player);
    }

    /**
     * Check if viewer should see target's nametag based on vanish system
     */
    private boolean shouldShowNametag(Player viewer, Player target) {
        // Use existing vanish system logic
        return viewer.canSee(target);
    }

    /**
     * Update all nametags for a specific viewer (called when their state changes)
     */
    public void updateNametagsFor(Player viewer) {
        for (PlayerNametag nametag : nametags.values()) {
            Player target = nametag.getPlayer();
            if (target != null && !target.equals(viewer)) {
                updateNametagForViewer(nametag, viewer);
            }
        }
    }

    /**
     * Update a specific nametag for a specific viewer
     */
    private void updateNametagForViewer(PlayerNametag nametag, Player viewer) {
        Player target = nametag.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(target.getUniqueId());

        if (practicePlayer == null) {
            return;
        }

        if (shouldShowNametag(viewer, target)) {
            NametagContent content = providerResolver.getNametagProviders()
                    .get(practicePlayer.getPlayerState())
                    .getContent(nametag, practicePlayer);
            nametag.updateFor(viewer, content);
        } else {
            nametag.hideFor(viewer);
        }
    }

    private final class NametagListener implements Listener {

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            PlayerNametag nametag = new PlayerNametag(player);
            nametags.put(playerUUID, nametag);

            // Update the new player's nametag for all existing players
            updateNametag(nametag);

            // Update all existing nametags for the new player
            updateNametagsFor(player);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            PlayerNametag nametag = nametags.remove(playerUUID);
            if (nametag != null) {
                nametag.delete();
            }
        }
    }
}