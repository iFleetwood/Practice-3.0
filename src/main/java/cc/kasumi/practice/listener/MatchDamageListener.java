package cc.kasumi.practice.listener;

import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.vanish.VanishUtil;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class MatchDamageListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player damager = null;
        Entity entityDamager = event.getDamager();

        // Handle direct player damage
        if (entityDamager instanceof Player playerDamager) {
            damager = playerDamager;
        }
        // Handle projectile damage
        else if (entityDamager instanceof Projectile projectile) {
            if (projectile.getShooter() instanceof Player playerShooter) {
                damager = playerShooter;
            }
        }

        if (damager == null) {
            return;
        }

        PracticePlayer damagerPracticePlayer = PracticePlayer.getPracticePlayer(damager.getUniqueId());
        PracticePlayer victimPracticePlayer = PracticePlayer.getPracticePlayer(victim.getUniqueId());

        if (damagerPracticePlayer == null || victimPracticePlayer == null) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if players shouldn't see each other (different matches, etc.)
        if (!VanishUtil.shouldSeeEachOther(damager, victim)) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if either player is not in a match (lobby, queue, editing, etc.)
        if (!damagerPracticePlayer.isInMatch() || !victimPracticePlayer.isInMatch()) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if players are in different matches
        if (damagerPracticePlayer.getCurrentMatch() != victimPracticePlayer.getCurrentMatch()) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if either player is spectating
        if (damagerPracticePlayer.getPlayerState() == PlayerState.SPECTATING || 
            victimPracticePlayer.getPlayerState() == PlayerState.SPECTATING) {
            event.setCancelled(true);
            return;
        }

        // At this point, both players are in the same match and actively playing
        // Allow the damage to proceed
    }
}