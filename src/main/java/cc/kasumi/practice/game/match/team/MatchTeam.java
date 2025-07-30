package cc.kasumi.practice.game.match.team;

import cc.kasumi.practice.game.match.player.MatchPlayer;
import org.bukkit.entity.Player;

import java.util.List;

public interface MatchTeam {

    int getAlive();
    MatchPlayer getLeader();
    List<MatchPlayer> getPlayers();
    List<Player> getBukkitPlayers();
}
