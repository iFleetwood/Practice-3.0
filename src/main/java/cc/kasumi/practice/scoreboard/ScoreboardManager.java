package cc.kasumi.practice.scoreboard;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.scoreboard.type.ProviderResolver;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;

@Getter
public class ScoreboardManager {

    private final Map<UUID, PlayerScoreboard> scoreboards = new ConcurrentHashMap<>();
    private final ProviderResolver providerResolver;
    private final BukkitTask bukkitTask;

    public ScoreboardManager(Practice plugin) {
        Bukkit.getPluginManager().registerEvents(new ScoreboardListener(), plugin);
        this.providerResolver = new ProviderResolver();
        this.bukkitTask = new ScoreboardRunnable(this).runTaskTimerAsynchronously(plugin, 20, 10);
    }

    public void updateTitle(PlayerScoreboard playerScoreboard) {
        playerScoreboard.updateTitle(MAIN_COLOR + "" + ChatColor.BOLD + "  Kasumi.cc");
    }

    public void updateScoreboard(PlayerScoreboard playerScoreboard) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(playerScoreboard.getPlayer().getUniqueId());

        playerScoreboard.updateLines(providerResolver.getScoreboardProviders().get(practicePlayer.getPlayerState()).getLines(playerScoreboard, practicePlayer));
    }

    private final class ScoreboardListener implements Listener {

        @EventHandler
        public void onPlayerJoin(PlayerJoinEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            PlayerScoreboard scoreboard = new PlayerScoreboard(player);
            updateTitle(scoreboard);
            updateScoreboard(scoreboard);

            scoreboards.put(playerUUID, scoreboard);
        }

        @EventHandler
        public void onPlayerQuit(PlayerQuitEvent event) {
            Player player = event.getPlayer();
            UUID playerUUID = player.getUniqueId();

            scoreboards.remove(playerUUID);
        }
    }
}
