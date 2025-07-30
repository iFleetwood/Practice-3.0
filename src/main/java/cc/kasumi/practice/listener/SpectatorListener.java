package cc.kasumi.practice.listener;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.spectator.SpectatorTeleportMenu;
import cc.kasumi.practice.vanish.VanishUtil;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public class SpectatorListener implements Listener {

    private final Map<UUID, PracticePlayer> players;

    public SpectatorListener(Practice plugin) {
        this.players = plugin.getPlayers();
    }

    @EventHandler
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

        // Cancel damage if damager is spectating
        if (damagerPracticePlayer != null && isSpectating(damagerPracticePlayer)) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if victim is spectating
        if (victimPracticePlayer != null && isSpectating(victimPracticePlayer)) {
            event.setCancelled(true);
            return;
        }

        // Cancel damage if players shouldn't see each other (different matches, etc.)
        if (!VanishUtil.shouldSeeEachOther(damager, victim)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        // Cancel all damage for spectators
        if (practicePlayer != null && isSpectating(practicePlayer)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        // Cancel item pickup for spectators
        if (practicePlayer != null && isSpectating(practicePlayer)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        // Handle spectator interactions
        if (practicePlayer != null && isSpectating(practicePlayer)) {
            ItemStack item = event.getItem();

            if (item != null) {
                // Handle spectator compass (teleport menu)
                if (item.getType() == Material.COMPASS) {
                    event.setCancelled(true);
                    new SpectatorTeleportMenu().openMenu(player);
                    return;
                }

                // Handle stop spectating item
                if (item.getType() == Material.REDSTONE) {
                    event.setCancelled(true);
                    // Execute the unspectate command
                    player.performCommand("spectate stop");
                    return;
                }
            }

            // Cancel other interactions for spectators
            switch (event.getAction()) {
                case LEFT_CLICK_BLOCK:
                case RIGHT_CLICK_BLOCK:
                case PHYSICAL: // Pressure plates, etc.
                    event.setCancelled(true);
                    break;
                case LEFT_CLICK_AIR:
                case RIGHT_CLICK_AIR:
                    // Allow air clicks for potential spectator GUIs (handled above)
                    break;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        // Cancel block breaking for spectators
        if (practicePlayer != null && isSpectating(practicePlayer)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        // Cancel block placing for spectators
        if (practicePlayer != null && isSpectating(practicePlayer)) {
            event.setCancelled(true);
        }
    }

    /**
     * Check if a player is currently spectating
     */
    private boolean isSpectating(PracticePlayer practicePlayer) {
        return practicePlayer.getPlayerState() == PlayerState.SPECTATING;
    }
}