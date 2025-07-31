package cc.kasumi.practice.game.match.team;

import cc.kasumi.practice.game.match.player.MatchPlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class TeamImpl implements MatchTeam {

    private final List<MatchPlayer> players;

    public TeamImpl(List<MatchPlayer> players) {
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Team must have at least one player");
        }
        this.players = new ArrayList<>(players);
    }

    @Override
    public int getAlive() {
        return (int) players.stream().filter(player -> !player.isDead()).count();
    }

    @Override
    public MatchPlayer getLeader() {
        return players.get(0); // First player is the leader
    }

    @Override
    public List<MatchPlayer> getPlayers() {
        return new ArrayList<>(players);
    }

    @Override
    public List<Player> getBukkitPlayers() {
        List<Player> bukkitPlayers = new ArrayList<>();
        
        for (MatchPlayer matchPlayer : players) {
            Player player = matchPlayer.getPlayer();
            if (player != null) {
                bukkitPlayers.add(player);
            }
        }
        
        return bukkitPlayers;
    }
}