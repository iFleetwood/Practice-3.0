package cc.kasumi.practice.game.match;

import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class MatchListener implements Listener {

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(uuid);

        if (practicePlayer.getPlayerState() != PlayerState.PLAYING) {
            return;
        }

        Match currentMatch = practicePlayer.getCurrentMatch();
        currentMatch.disconnected(player);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        if (practicePlayer.getPlayerState() != PlayerState.PLAYING) {
            return;
        }

        MatchState matchState = practicePlayer.getCurrentMatch().getMatchState();
        if (matchState == MatchState.ENDING || matchState == MatchState.CANCELED) {
            event.setCancelled(true);
        }

        if (player.getHealth() <= event.getFinalDamage()) {
            Match match = practicePlayer.getCurrentMatch();
            match.handleDeath(player, player.getLocation(), true);
            event.setCancelled(true);
        }
    }
}
