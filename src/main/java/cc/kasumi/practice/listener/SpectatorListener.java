package cc.kasumi.practice.listener;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.Map;
import java.util.UUID;

public class SpectatorListener implements Listener {

    private final Map<UUID, PracticePlayer> players;

    public SpectatorListener(Practice plugin) {
        this.players = plugin.getPlayers();
    }

    /*
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (!practicePlayer.isInMatch()) {
            event.setCancelled(true);
            return;
        }

        Entity entityDamager = event.getDamager();
        Player damager;

        if (entityDamager instanceof Player playerDamager) {
            damager = playerDamager;

        } else if (entityDamager instanceof Projectile projectileDamager) {
            if (!(projectileDamager.getShooter() instanceof Player playerDamager)) {
                return;
            }

            damager = playerDamager;

        } else {
            return;
        }

        PracticePlayer damagerPracticePlayer = PracticePlayer.getPracticePlayer(damager.getUniqueId());

        if (!isSpectating(damagerPracticePlayer)) {
            return;
        }

        event.setCancelled(true);
    }

     */

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (!practicePlayer.isSpectating()) {
            return;
        }

        event.setCancelled(true);
    }
}
