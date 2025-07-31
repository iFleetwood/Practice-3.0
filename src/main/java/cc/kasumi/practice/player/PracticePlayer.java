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
        
        // Initialize default ratings for all ladders
        initializeLadderRatings();
    }
    
    private void initializeLadderRatings() {
        // Initialize ratings for all existing ladders
        for (Ladder ladder : Practice.getInstance().getLadders().values()) {
            if (!ladderRatings.containsKey(ladder)) {
                ladderRatings.put(ladder, new PlayerElo(1000));
            }
        }
    }
    
    /**
     * Get ELO rating for a specific ladder
     */
    public PlayerElo getLadderElo(Ladder ladder) {
        return ladderRatings.computeIfAbsent(ladder, k -> new PlayerElo(1000));
    }
    
    /**
     * Set ELO rating for a specific ladder
     */
    public void setLadderElo(Ladder ladder, PlayerElo elo) {
        ladderRatings.put(ladder, elo);
    }
    
    /**
     * Get overall ELO (average of all ladder ratings)
     */
    public PlayerElo getElo() {
        if (ladderRatings.isEmpty()) {
            return elo; // Fallback to global elo
        }
        
        int totalRating = ladderRatings.values().stream()
                .mapToInt(PlayerElo::getRating)
                .sum();
        int averageRating = totalRating / ladderRatings.size();
        
        return new PlayerElo(averageRating);
    }
    
    /**
     * Get ladder-specific ELO rating as integer for convenience
     */
    public int getLadderRating(Ladder ladder) {
        return getLadderElo(ladder).getRating();
    }
    
    /**
     * Get all ladder ratings for display
     */
    public Map<Ladder, PlayerElo> getLadderRatings() {
        return new HashMap<>(ladderRatings);
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
            // Load global elo for backward compatibility
            if (result.containsKey("elo")) {
                elo = new PlayerElo(result.getInteger("elo"));
            }
            
            // Load per-ladder ratings
            if (result.containsKey("ladderRatings")) {
                Document ladderRatingsDoc = result.get("ladderRatings", Document.class);
                for (String ladderName : ladderRatingsDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        int rating = ladderRatingsDoc.getInteger(ladderName, 1000);
                        ladderRatings.put(ladder, new PlayerElo(rating));
                    }
                }
            }
            
            // Initialize any missing ladder ratings
            initializeLadderRatings();
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
        Document doc = getKey()
                .append("elo", elo.getRating());
        
        // Save per-ladder ratings
        Document ladderRatingsDoc = new Document();
        for (Map.Entry<Ladder, PlayerElo> entry : ladderRatings.entrySet()) {
            ladderRatingsDoc.append(entry.getKey().getName(), entry.getValue().getRating());
        }
        doc.append("ladderRatings", ladderRatingsDoc);
        
        return doc;
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
