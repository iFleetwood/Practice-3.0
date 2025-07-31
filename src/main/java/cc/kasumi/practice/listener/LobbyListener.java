package cc.kasumi.practice.listener;

import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.UUID;

public class LobbyListener implements Listener {

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (player.isOp()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            return;
        }

        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.isInMatch()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockPlace(BlockBreakEvent event) {
        Player player = event.getPlayer();

        if (player.isOp()) {
            return;
        }

        event.setCancelled(true);
    }
}
