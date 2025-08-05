package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.menu.KitEditorMenu;
import cc.kasumi.practice.menu.SelectFFAQueueMenu;
import cc.kasumi.practice.menu.SelectQueueMenu;
import cc.kasumi.practice.menu.SelectTvTQueueMenu;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import cc.kasumi.practice.util.PlayerItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class QueueListener implements Listener {

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack clicked = event.getItem();

        if (clicked == null) {
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);
        if (practicePlayer == null) {
            return;
        }

        // Don't allow interactions if player is not in lobby
        if (practicePlayer.getPlayerState() != PlayerState.LOBBY) {
            return;
        }

        if (clicked.equals(PlayerItem.KIT_EDITOR.getItem())) {
            // Open kit editor menu
            new KitEditorMenu().openMenu(player);
        }

        else if (clicked.equals(PlayerItem.RANKED_QUEUE.getItem())) {
            // Open ranked queue menu
            new SelectQueueMenu(true).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.UNRANKED_QUEUE.getItem())) {
            new SelectQueueMenu(false).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.FFA_RANKED_QUEUE.getItem())) {
            new SelectFFAQueueMenu(true).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.FFA_UNRANKED_QUEUE.getItem())) {
            new SelectFFAQueueMenu(false).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.TVT_RANKED_QUEUE.getItem())) {
            new SelectTvTQueueMenu(true).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.TVT_UNRANKED_QUEUE.getItem())) {
            new SelectTvTQueueMenu(false).openMenu(player);
        }

        else if (clicked.equals(PlayerItem.LEAVE_QUEUE.getItem())) {
            if (practicePlayer.getPlayerState() != PlayerState.QUEUEING) {
                return;
            }

            Queue queue = practicePlayer.getCurrentQueue();

            queue.removePlayerByUUID(uuid);
            practicePlayer.setCurrentQueue(null);
            practicePlayer.setPlayerState(PlayerState.LOBBY);
            player.getInventory().setContents(GameUtil.getLobbyContents());

            player.sendMessage("§cLeft the queue!");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer == null) {
            return;
        }

        if (practicePlayer.getPlayerState() != PlayerState.QUEUEING) {
            return;
        }

        practicePlayer.getCurrentQueue().removePlayerByUUID(uuid);
        practicePlayer.setCurrentQueue(null);
    }
}