package cc.kasumi.practice.listener;

import cc.kasumi.commons.util.MessageUtil;
import cc.kasumi.commons.util.PlayerUtil;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import cc.kasumi.practice.vanish.VanishUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static cc.kasumi.practice.PracticeConfiguration.*;
import cc.kasumi.practice.nametag.NametagManager;
import cc.kasumi.practice.nametag.PlayerNametag;

public class PlayerListener implements Listener {

    private final Practice plugin;
    private final Map<UUID, PracticePlayer> players;

    public PlayerListener(Practice plugin) {
        this.plugin = plugin;
        this.players = plugin.getPlayers();
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        if (!plugin.isConnectionEstablished()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ERROR_COLOR + "Database connection isn't established");
            return;
        }

        UUID uuid = event.getUniqueId();
        CompletableFuture<PracticePlayer> practicePlayer = new PracticePlayer(uuid).load();

        try {
            players.put(uuid, practicePlayer.get());
        } catch (Exception e) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, ERROR_COLOR + "Failed to load your data, please try to rejoin!");

            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        event.setJoinMessage(MAIN_COLOR + player.getName() + SEC_COLOR + " joined the server!");

        player.teleport(player.getWorld().getSpawnLocation());
        PlayerUtil.resetPlayer(player);
        player.getInventory().setContents(GameUtil.getLobbyContents());

        // Use enhanced vanish system for automatic visibility management
        VanishUtil.updatePlayerVanish(player);
        
        // Update nametags after a short delay to ensure PracticePlayer is available
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            NametagManager nametagManager = Practice.getInstance().getNametagManager();
            nametagManager.updateNametagsFor(player);
            
            // Also update this player's nametag for others
            PlayerNametag playerNametag = nametagManager.getNametags().get(player.getUniqueId());
            if (playerNametag != null) {
                nametagManager.updateNametag(playerNametag);
            }
        }, 10L); // 0.5 second delay
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        event.setQuitMessage(MAIN_COLOR + player.getName() + SEC_COLOR + " left the server!");

        if (!players.containsKey(uuid)) {
            return;
        }

        players.get(uuid).save(true);
        players.remove(uuid);

        // Enhanced vanish cleanup is automatically handled by VanishListener
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        event.setFormat(MessageUtil.color(MAIN_COLOR + "%1$s" + SEC_COLOR + ": %2$s"));
    }
}
