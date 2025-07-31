package cc.kasumi.practice.listener;

import cc.kasumi.practice.util.BukkitUtil;
import cc.kasumi.practice.util.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

public class EnderpearlListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void corretTeleport(PlayerTeleportEvent event) {
        // Do nothing if this module is not enabled.

        // Do nothing if not teleported by an enderpearl.
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.ENDER_PEARL) return;

        // Calculate safe teleportation location and change the destination.
        Location to = event.getTo();
        Block block = to.getBlock();
        if (!MaterialUtil.isFullBlock(block.getType())) {
            event.setTo(event.getTo().subtract(0, to.getY() - (int) to.getY(), 0));
        }

        if (MaterialUtil.isFullBlock(block.getRelative(BlockFace.UP).getType())) {
            event.setTo(to.subtract(0, 1, 0));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void denyEnderpearlBlocks(PlayerInteractEvent event) {

        // Do nothing if player has permission.
        Player player = event.getPlayer();

        // Do nothing if player is not clicking an enderpearl.
        if (!BukkitUtil.hasItemSelected(player, Material.ENDER_PEARL)) return;

        // Cancel the event if player clicked a block.
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            return;
        }

        // Do nothing if player did not right click air.
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        // Do nothing if player is not within a solid block.
        Block block = player.getLocation().getBlock();
        if (!MaterialUtil.isFullBlock(block.getRelative(BlockFace.UP).getType()) &&
                !MaterialUtil.isFullBlock(block.getType())) {
            return;
        }

        // Cancel the event and inform the player.
        event.setCancelled(true);
    }

    /*
    @EventHandler(priority = EventPriority.NORMAL)
    public void denyEnderpearlCooldown(PlayerInteractEvent event) {
        // Do nothing if enderpearl cooldown is invalid.
        if (settings.getCooldown() <= 0) return;

        // Do nothing if player did not right click air.
        if (event.getAction() != Action.RIGHT_CLICK_AIR) return;

        // Do nothing if player has permission.
        Player player = event.getPlayer();
        if (player.hasPermission(Perm.AntiGlitch.ENDERPEARLS_COOLDOWN)) return;

        // Do nothing if player is not clicking an enderpearl.
        if (!BukkitUtils.hasItemSelected(player, Material.ENDER_PEARL)) return;

        // Deny event if user is already throwing an enderpearl.
        User user = plugin.getOrCreateUser(player.getUniqueId());
        if (user.isThrowingPearl()) {
            event.setCancelled(true);
            MessageUtils.sendMessage(player, settings.getMultipleMessage());
            return;
        }

        // Do nothing if the user is not on a cooldown.
        long enderpearl = DurationUtils.calculateRemaining(user.getEnderpearlCooldown());
        long door = DurationUtils.calculateRemaining(user.getEnderpearlDoorCooldown());
        if (enderpearl < 0 && door < 0) return;

        // Deny the event and send player a message.
        event.setCancelled(true);
        MessageUtils.sendMessage(player, settings.getCooldownMessage()
                .replace("{time}", DurationUtils.format(enderpearl > door ? enderpearl : door)));
    }

     */

    /*
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void setEnderpearlCooldown(PlayerTeleportEvent event) {
        // Do nothing if not teleported by an enderpearl.
        if (event.getCause() != TeleportCause.ENDER_PEARL) return;

        // Do nothing if player has permission.
        Player player = event.getPlayer();
        if (player.hasPermission(Perm.AntiGlitch.ENDERPEARLS_COOLDOWN)) return;

        // Set the users' enderpearl cooldown.
        User user = plugin.getOrCreateUser(player.getUniqueId());
        user.setEnderpearlCooldown(System.currentTimeMillis() + settings.getCooldown());
    }

     */

    /*
    @EventHandler(priority = EventPriority.MONITOR)
    public void setDoorCooldown(PlayerInteractEvent event) {
        // Do nothing if there is no door cooldown.
        if (settings.getDoorCooldown() <= 0) return;

        // Do nothing if player has not right clicked a block.
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // Do nothing if player has permission.
        Player player = event.getPlayer();
        if (player.hasPermission(Perm.AntiGlitch.ENDERPEARLS_COOLDOWN)) return;

        // Do nothing if player did not click a door.
        if (!MaterialUtils.isDoorBlock(event.getClickedBlock().getType())) return;

        // Give player a door enderpearl cooldown.
        User user = plugin.getOrCreateUser(player.getUniqueId());
        user.setEnderpearlDoorCooldown(System.currentTimeMillis() + settings.getDoorCooldown());
    }

     */

    /*
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void setCurrentPearl(ProjectileLaunchEvent event) {
        // Do nothing if entity that died was not an enderpearl.
        if (event.getEntityType() != EntityType.ENDER_PEARL) return;

        // Do nothing if shooter was not a player.
        EnderPearl enderpearl = (EnderPearl) event.getEntity();
        ProjectileSource shooter = enderpearl.getShooter();
        if (!(shooter instanceof Player)) return;

        // Do nothing if user has not thrown an enderpearl.
        User user = plugin.getUsers().get(((Player) shooter).getUniqueId());
        if (user == null) return;

        // User is no longer throwing an enderpearl.
        user.setPearl(new WeakReference<>(enderpearl));
    }

     */
}
