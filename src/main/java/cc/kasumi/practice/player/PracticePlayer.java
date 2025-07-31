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

    // Core player data
    private final UUID uuid;
    private PlayerState playerState = PlayerState.LOBBY;
    private PlayerElo elo = new PlayerElo(1000);
    private Map<Ladder, PlayerElo> ladderRatings = new HashMap<>();
    
    // Additional statistics for MongoDB
    private Map<Ladder, Integer> ladderWins = new HashMap<>();
    private Map<Ladder, Integer> ladderLosses = new HashMap<>();
    private Map<Ladder, Integer> ladderWinStreaks = new HashMap<>();
    private Map<Ladder, Integer> ladderBestWinStreaks = new HashMap<>();
    private int totalKills = 0;
    private int totalDeaths = 0;
    private long firstJoined = System.currentTimeMillis();
    private long lastSeen = System.currentTimeMillis();

    private final Map<Ladder, PlayerKit> ladderKits = new HashMap<>();
    private final Map<UUID, DuelRequest> duelRequests = new HashMap<>();

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
        
        // Initialize default ratings and statistics for all ladders
        initializeLadderRatings();
        initializeLadderStats();
    }
    
    private void initializeLadderRatings() {
        // Initialize ratings for all existing ladders
        for (Ladder ladder : Practice.getInstance().getLadders().values()) {
            if (!ladderRatings.containsKey(ladder)) {
                ladderRatings.put(ladder, new PlayerElo(1000));
            }
        }
    }
    
    private void initializeLadderStats() {
        // Initialize statistics for all existing ladders
        for (Ladder ladder : Practice.getInstance().getLadders().values()) {
            ladderWins.putIfAbsent(ladder, 0);
            ladderLosses.putIfAbsent(ladder, 0);
            ladderWinStreaks.putIfAbsent(ladder, 0);
            ladderBestWinStreaks.putIfAbsent(ladder, 0);
        }
    }
    
    /**
     * Record a win for a specific ladder
     */
    public void recordWin(Ladder ladder) {
        ladderWins.put(ladder, ladderWins.getOrDefault(ladder, 0) + 1);
        int currentStreak = ladderWinStreaks.getOrDefault(ladder, 0) + 1;
        ladderWinStreaks.put(ladder, currentStreak);
        
        // Update best win streak if current is better
        int bestStreak = ladderBestWinStreaks.getOrDefault(ladder, 0);
        if (currentStreak > bestStreak) {
            ladderBestWinStreaks.put(ladder, currentStreak);
        }
    }
    
    /**
     * Record a loss for a specific ladder
     */
    public void recordLoss(Ladder ladder) {
        ladderLosses.put(ladder, ladderLosses.getOrDefault(ladder, 0) + 1);
        ladderWinStreaks.put(ladder, 0); // Reset win streak
    }
    
    /**
     * Record a kill
     */
    public void recordKill() {
        totalKills++;
    }
    
    /**
     * Record a death
     */
    public void recordDeath() {
        totalDeaths++;
    }
    
    /**
     * Get win rate for a specific ladder
     */
    public double getWinRate(Ladder ladder) {
        int wins = ladderWins.getOrDefault(ladder, 0);
        int losses = ladderLosses.getOrDefault(ladder, 0);
        int total = wins + losses;
        
        if (total == 0) return 0.0;
        return (double) wins / total * 100.0;
    }
    
    /**
     * Get kill/death ratio
     */
    public double getKDRatio() {
        if (totalDeaths == 0) return totalKills;
        return (double) totalKills / totalDeaths;
    }
    
    /**
     * Get total matches played for a ladder
     */
    public int getTotalMatches(Ladder ladder) {
        return ladderWins.getOrDefault(ladder, 0) + ladderLosses.getOrDefault(ladder, 0);
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
            
            // Load basic statistics
            totalKills = result.getInteger("totalKills", 0);
            totalDeaths = result.getInteger("totalDeaths", 0);
            firstJoined = result.getLong("firstJoined", System.currentTimeMillis());
            lastSeen = result.getLong("lastSeen", System.currentTimeMillis());
            
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
            
            // Load per-ladder wins
            if (result.containsKey("ladderWins")) {
                Document ladderWinsDoc = result.get("ladderWins", Document.class);
                for (String ladderName : ladderWinsDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderWins.put(ladder, ladderWinsDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder losses
            if (result.containsKey("ladderLosses")) {
                Document ladderLossesDoc = result.get("ladderLosses", Document.class);
                for (String ladderName : ladderLossesDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderLosses.put(ladder, ladderLossesDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder win streaks
            if (result.containsKey("ladderWinStreaks")) {
                Document ladderWinStreaksDoc = result.get("ladderWinStreaks", Document.class);
                for (String ladderName : ladderWinStreaksDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderWinStreaks.put(ladder, ladderWinStreaksDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder best win streaks
            if (result.containsKey("ladderBestWinStreaks")) {
                Document ladderBestWinStreaksDoc = result.get("ladderBestWinStreaks", Document.class);
                for (String ladderName : ladderBestWinStreaksDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderBestWinStreaks.put(ladder, ladderBestWinStreaksDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Initialize any missing ladder data
            initializeLadderRatings();
            initializeLadderStats();
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
                .append("elo", elo.getRating())
                .append("totalKills", totalKills)
                .append("totalDeaths", totalDeaths)
                .append("firstJoined", firstJoined)
                .append("lastSeen", System.currentTimeMillis()); // Update last seen on save
        
        // Save per-ladder ratings
        Document ladderRatingsDoc = new Document();
        for (Map.Entry<Ladder, PlayerElo> entry : ladderRatings.entrySet()) {
            ladderRatingsDoc.append(entry.getKey().getName(), entry.getValue().getRating());
        }
        doc.append("ladderRatings", ladderRatingsDoc);
        
        // Save per-ladder wins
        Document ladderWinsDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderWins.entrySet()) {
            ladderWinsDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderWins", ladderWinsDoc);
        
        // Save per-ladder losses
        Document ladderLossesDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderLosses.entrySet()) {
            ladderLossesDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderLosses", ladderLossesDoc);
        
        // Save per-ladder win streaks
        Document ladderWinStreaksDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderWinStreaks.entrySet()) {
            ladderWinStreaksDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderWinStreaks", ladderWinStreaksDoc);
        
        // Save per-ladder best win streaks
        Document ladderBestWinStreaksDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderBestWinStreaks.entrySet()) {
            ladderBestWinStreaksDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderBestWinStreaks", ladderBestWinStreaksDoc);
        
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
