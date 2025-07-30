package cc.kasumi.practice.game.duel;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.player.PracticePlayer;
import lombok.NonNull;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DuelManager {

    private final Practice plugin;

    public DuelManager(Practice plugin) {
        this.plugin = plugin;
    }

    public void sendDuelRequest(UUID senderUUID, PracticePlayer target, Ladder ladder) {
        target.getDuelRequests().put(senderUUID, new DuelRequest(senderUUID, ladder));
    }

    public void acceptDuel(Player player, Player target , @NonNull Ladder ladder) {
        plugin.getMatchManager().createSoloMatch(player, target, ladder, plugin.getArenaManager().getAvailableArena(), false);
    }
}
