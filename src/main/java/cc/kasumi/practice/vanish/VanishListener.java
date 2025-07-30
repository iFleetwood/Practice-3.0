package cc.kasumi.practice.vanish;

import cc.kasumi.practice.Practice;
import com.destroystokyo.paper.event.entity.ProjectileCollideEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.UUID;

public class VanishListener implements Listener {

    private final VanishManager vanishManager;
    private final Practice plugin;

    public VanishListener(Practice plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;

        // Start cleanup task to prevent memory leaks
        startCleanupTask();
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        int id = event.getEntity().getEntityId();
        vanishManager.registerSource(id, shooter.getUniqueId());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        vanishManager.unregisterSource(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onProjectileCollide(ProjectileCollideEvent event) {
        if (!(event.getCollidedWith() instanceof Player collided)) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        if (!vanishManager.shouldShowSource(shooter.getUniqueId(), collided.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        int id = event.getItemDrop().getEntityId();
        vanishManager.registerSource(id, player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        int id = event.getItem().getEntityId();

        if (event.isCancelled()) {
            return;
        }

        UUID sourceUUID = vanishManager.getSources().get(id);
        if (sourceUUID == null) {
            return;
        }

        if (!vanishManager.shouldShowSource(sourceUUID, player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        vanishManager.unregisterSource(id);
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        vanishManager.unregisterSource(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        if (!(potion.getShooter() instanceof Player shooter)) {
            return;
        }

        // Register particle location
        Location location = potion.getLocation();
        Location blockLocation = new Location(location.getWorld(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
        vanishManager.registerParticle(blockLocation, shooter.getUniqueId());

        // Clean up particle location after a tick
        new BukkitRunnable() {
            @Override
            public void run() {
                vanishManager.getParticles().remove(blockLocation);
            }
        }.runTaskLater(plugin, 1L);

        // Filter potion effects based on visibility
        Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
        for (LivingEntity affectedEntity : affectedEntities) {
            if (!(affectedEntity instanceof Player affectedPlayer)) {
                continue;
            }

            if (!vanishManager.shouldShowSource(shooter.getUniqueId(), affectedPlayer.getUniqueId())) {
                event.setIntensity(affectedEntity, 0);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up all vanish data for leaving player
        vanishManager.clearPlayer(event.getPlayer().getUniqueId());
    }

    /**
     * Periodic cleanup to prevent memory leaks
     */
    private void startCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                // Clean up particles older than 5 seconds
                vanishManager.cleanupParticles();

                // Could add more cleanup logic here
            }
        }.runTaskTimer(plugin, 100L, 100L); // Every 5 seconds
    }
}