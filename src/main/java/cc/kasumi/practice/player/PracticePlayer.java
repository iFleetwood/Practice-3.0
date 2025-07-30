package cc.kasumi.practice.player;

import cc.kasumi.commons.mongodb.MCollection;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.duel.DuelRequest;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.vanish.VanishUtil;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Getter
public class PracticePlayer {

    private final Map<Ladder, PlayerElo> ladderRatings = new HashMap<>();
    private final Map<Ladder, PlayerKit> ladderKits = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();

    private final UUID uuid;

    private PlayerElo elo;

    private PlayerState playerState;
    @Setter
    private Queue currentQueue;
    private Match currentMatch;
    private Match spectatingMatch;

    @Setter
    private boolean builder;

    public PracticePlayer(UUID uuid) {
        this.uuid = uuid;
        this.playerState = PlayerState.LOBBY;
        this.elo = new PlayerElo(1000);
        this.builder = false;
    }

    public CompletableFuture<PracticePlayer> load() {
        long timestamp = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            MCollection playerCollection = Practice.getInstance().getPlayersCollection();

            loadResult(playerCollection.getDocument(getKey()));
            Bukkit.broadcastMessage(ChatColor.GREEN + uuid.toString() + " took " + (System.currentTimeMillis() - timestamp) + " ms to load!");

            return PracticePlayer.this;
        });
    }

    private void loadResult(Document result) {
        if (result != null) {
            elo = new PlayerElo(result.getInteger("elo"));
        } else {
            save(true);
        }
    }

    public void save(boolean async) {
        MCollection playerCollection = Practice.getInstance().getPlayersCollection();

        if (async) {
            playerCollection.updateDocumentAsync(getKey(), getPlayerDocument());
        } else {
            playerCollection.updateDocument(getKey(), getPlayerDocument());
        }
    }

    public Document getPlayerDocument() {
        return getKey()
                .append("elo", elo.getRating());
    }

    public Document getKey() {
        return new Document("uuid", uuid.toString());
    }

    public boolean isInLobby() {
        return playerState == PlayerState.LOBBY;
    }

    public boolean isInMatch() {
        return playerState == PlayerState.PLAYING;
    }

    public boolean isBusy() {
        return playerState != PlayerState.LOBBY;
    }

    public boolean isSpectating() {
        return playerState == PlayerState.SPECTATING;
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(uuid);
    }

    public static PracticePlayer getPracticePlayer(UUID uuid) {
        return Practice.getInstance().getPlayers().get(uuid);
    }

    // Enhanced setters with vanish integration
    public void setPlayerState(PlayerState newState) {
        PlayerState oldState = this.playerState;
        this.playerState = newState;

        // Update vanish when player state changes
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            VanishUtil.updatePlayerVanish(player);
        }
    }

    public void setCurrentMatch(Match match) {
        this.currentMatch = match;

        // Update vanish when match changes
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            VanishUtil.updatePlayerVanish(player);
        }
    }

    public void setSpectatingMatch(Match match) {
        this.spectatingMatch = match;

        // Update vanish when spectating changes
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            VanishUtil.updatePlayerVanish(player);
        }
    }
}
