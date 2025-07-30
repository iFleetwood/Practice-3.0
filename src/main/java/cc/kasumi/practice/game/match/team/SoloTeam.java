package cc.kasumi.practice.game.match.team;

import cc.kasumi.practice.game.match.player.MatchPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SoloTeam implements MatchTeam {

    private final MatchPlayer matchPlayer;

    public SoloTeam(MatchPlayer matchPlayer) {
        this.matchPlayer = matchPlayer;
    }

    @Override
    public int getAlive() {
        return !matchPlayer.isDead() ? 1 : 0;
    }

    @Override
    public MatchPlayer getLeader() {
        return matchPlayer;
    }

    @Override
    public List<MatchPlayer> getPlayers() {
        return List.of(matchPlayer);
    }

    @Override
    public List<Player> getBukkitPlayers() {
        List<Player> bukkitPlayers = new ArrayList<>();

        Player player = matchPlayer.getPlayer();
        if (matchPlayer.getPlayer() != null) {
            bukkitPlayers.add(player);
        }

        return bukkitPlayers;
    }
}
