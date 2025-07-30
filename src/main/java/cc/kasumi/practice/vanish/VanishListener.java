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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class VanishListener implements Listener {

    private final Map<Integer, UUID> sources;
    private final Map<Location, UUID> particles;
    private final VanishManager vanishManager;
    private final Practice plugin;

    public VanishListener(Practice plugin, VanishManager vanishManager) {
        this.plugin = plugin;
        this.vanishManager = vanishManager;
        this.sources = vanishManager.getSources();
        this.particles = vanishManager.getParticles();
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        int id = event.getEntity().getEntityId();
        sources.put(id, shooter.getUniqueId());
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        sources.remove(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onProjectileCollide(ProjectileCollideEvent event) {
        if (!(event.getCollidedWith() instanceof Player collided)) {
            return;
        }

        if (!(event.getEntity().getShooter() instanceof Player shooter)) {
            return;
        }

        if (shooter.canSee(collided)) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        int id = event.getItemDrop().getEntityId();
        sources.put(id, player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        int id = event.getItem().getEntityId();

        if (event.isCancelled()) {
            return;
        }

        if (!sources.containsKey(id)) {
            return;
        }

        UUID sourceUUID = sources.get(id);
        Player sourcePlayer = Bukkit.getPlayer(sourceUUID);

        if (sourcePlayer == null || player.canSee(sourcePlayer)) {
            sources.remove(id);
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onItemDespawn(ItemDespawnEvent event) {
        sources.remove(event.getEntity().getEntityId());
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent event) {
        ThrownPotion potion = event.getPotion();

        if (!(potion.getShooter() instanceof Player shooter)) {
            return;
        }

        // For cancelling packet
        Location location = potion.getLocation();
        Location blockLocation = new Location(location.getWorld(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        particles.put(blockLocation, shooter.getUniqueId());

        new BukkitRunnable() {
            @Override
            public void run() {
                particles.remove(blockLocation);
            }
        }.runTaskLater(plugin, 1L);

        // Removing healing from potion if shooter can't see affected player
        Collection<LivingEntity> affectedEntities = event.getAffectedEntities();
        for (LivingEntity affectedEntity : affectedEntities) {
            if (!(affectedEntity instanceof Player affectedPlayer)) {
                continue;
            }

            if (shooter.canSee(affectedPlayer)) {
                continue;
            }

            event.setIntensity(affectedEntity, 0);
        }
    }
}
