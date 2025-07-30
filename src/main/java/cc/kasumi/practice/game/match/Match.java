package cc.kasumi.practice.game.match;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.cache.CacheManager;
import cc.kasumi.practice.game.match.team.MatchTeam;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;

@Getter
public abstract class Match {

    protected final MatchManager matchManager = Practice.getInstance().getMatchManager();
    protected final CacheManager cacheManager = Practice.getInstance().getCacheManager();

    public abstract Set<MatchTeam> getTeams();
    protected final Set<UUID> spectators = new HashSet<>();

    protected final UUID identifier;
    protected final MatchType matchType;
    protected final Ladder ladder;
    protected final Arena arena;
    protected final boolean ranked;
    protected long timestamp;

    public abstract void handleDeath(Player player, Location location, boolean hidePlayer);
    public abstract void disconnected(Player player);

    @Setter
    protected MatchState matchState = MatchState.STARTING;

    protected Match(MatchType matchType, Ladder ladder, Arena arena, boolean ranked) {
        this.identifier = UUID.randomUUID();
        this.matchType = matchType;
        this.ladder = ladder;
        this.arena = arena;
        this.ranked = ranked;
    }
}
