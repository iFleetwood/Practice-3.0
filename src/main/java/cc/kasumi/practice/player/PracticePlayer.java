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
    private Map<Ladder, Integer> ladderRankedWins = new HashMap<>();
    private Map<Ladder, Integer> ladderRankedLosses = new HashMap<>();
    private Map<Ladder, Integer> ladderUnrankedWins = new HashMap<>();
    private Map<Ladder, Integer> ladderUnrankedLosses = new HashMap<>();
    private Map<Ladder, Integer> ladderWinStreaks = new HashMap<>();
    private Map<Ladder, Integer> ladderBestWinStreaks = new HashMap<>();
    private Map<Ladder, Integer> ladderRankedWinStreaks = new HashMap<>();
    private Map<Ladder, Integer> ladderBestRankedWinStreaks = new HashMap<>();
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
            ladderRankedWins.putIfAbsent(ladder, 0);
            ladderRankedLosses.putIfAbsent(ladder, 0);
            ladderUnrankedWins.putIfAbsent(ladder, 0);
            ladderUnrankedLosses.putIfAbsent(ladder, 0);
            ladderWinStreaks.putIfAbsent(ladder, 0);
            ladderBestWinStreaks.putIfAbsent(ladder, 0);
            ladderRankedWinStreaks.putIfAbsent(ladder, 0);
            ladderBestRankedWinStreaks.putIfAbsent(ladder, 0);
        }
    }
    
    /**
     * Record a win for a specific ladder
     */
    public void recordWin(Ladder ladder, boolean ranked) {
        // Update total wins
        ladderWins.put(ladder, ladderWins.getOrDefault(ladder, 0) + 1);
        
        // Update ranked/unranked specific wins
        if (ranked) {
            ladderRankedWins.put(ladder, ladderRankedWins.getOrDefault(ladder, 0) + 1);
            
            // Update ranked win streak
            int currentRankedStreak = ladderRankedWinStreaks.getOrDefault(ladder, 0) + 1;
            ladderRankedWinStreaks.put(ladder, currentRankedStreak);
            
            // Update best ranked win streak if current is better
            int bestRankedStreak = ladderBestRankedWinStreaks.getOrDefault(ladder, 0);
            if (currentRankedStreak > bestRankedStreak) {
                ladderBestRankedWinStreaks.put(ladder, currentRankedStreak);
            }
        } else {
            ladderUnrankedWins.put(ladder, ladderUnrankedWins.getOrDefault(ladder, 0) + 1);
        }
        
        // Update overall win streak
        int currentStreak = ladderWinStreaks.getOrDefault(ladder, 0) + 1;
        ladderWinStreaks.put(ladder, currentStreak);
        
        // Update best overall win streak if current is better
        int bestStreak = ladderBestWinStreaks.getOrDefault(ladder, 0);
        if (currentStreak > bestStreak) {
            ladderBestWinStreaks.put(ladder, currentStreak);
        }
    }
    
    /**
     * Record a loss for a specific ladder
     */
    public void recordLoss(Ladder ladder, boolean ranked) {
        // Update total losses
        ladderLosses.put(ladder, ladderLosses.getOrDefault(ladder, 0) + 1);
        
        // Update ranked/unranked specific losses
        if (ranked) {
            ladderRankedLosses.put(ladder, ladderRankedLosses.getOrDefault(ladder, 0) + 1);
            ladderRankedWinStreaks.put(ladder, 0); // Reset ranked win streak
        } else {
            ladderUnrankedLosses.put(ladder, ladderUnrankedLosses.getOrDefault(ladder, 0) + 1);
        }
        
        // Reset overall win streak
        ladderWinStreaks.put(ladder, 0);
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
     * Get ranked win rate for a specific ladder
     */
    public double getRankedWinRate(Ladder ladder) {
        int wins = ladderRankedWins.getOrDefault(ladder, 0);
        int losses = ladderRankedLosses.getOrDefault(ladder, 0);
        int total = wins + losses;
        
        if (total == 0) return 0.0;
        return (double) wins / total * 100.0;
    }
    
    /**
     * Get unranked win rate for a specific ladder
     */
    public double getUnrankedWinRate(Ladder ladder) {
        int wins = ladderUnrankedWins.getOrDefault(ladder, 0);
        int losses = ladderUnrankedLosses.getOrDefault(ladder, 0);
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
     * Get total ranked matches played for a ladder
     */
    public int getTotalRankedMatches(Ladder ladder) {
        return ladderRankedWins.getOrDefault(ladder, 0) + ladderRankedLosses.getOrDefault(ladder, 0);
    }
    
    /**
     * Get total unranked matches played for a ladder
     */
    public int getTotalUnrankedMatches(Ladder ladder) {
        return ladderUnrankedWins.getOrDefault(ladder, 0) + ladderUnrankedLosses.getOrDefault(ladder, 0);
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
            
            // Handle timestamps safely
            if (result.containsKey("firstJoined")) {
                Object firstJoinedObj = result.get("firstJoined");
                if (firstJoinedObj instanceof Long) {
                    firstJoined = (Long) firstJoinedObj;
                } else if (firstJoinedObj instanceof Integer) {
                    firstJoined = ((Integer) firstJoinedObj).longValue();
                } else {
                    firstJoined = System.currentTimeMillis();
                }
            } else {
                firstJoined = System.currentTimeMillis();
            }
            
            if (result.containsKey("lastSeen")) {
                Object lastSeenObj = result.get("lastSeen");
                if (lastSeenObj instanceof Long) {
                    lastSeen = (Long) lastSeenObj;
                } else if (lastSeenObj instanceof Integer) {
                    lastSeen = ((Integer) lastSeenObj).longValue();
                } else {
                    lastSeen = System.currentTimeMillis();
                }
            } else {
                lastSeen = System.currentTimeMillis();
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
            
            // Load per-ladder ranked wins
            if (result.containsKey("ladderRankedWins")) {
                Document ladderRankedWinsDoc = result.get("ladderRankedWins", Document.class);
                for (String ladderName : ladderRankedWinsDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderRankedWins.put(ladder, ladderRankedWinsDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder ranked losses
            if (result.containsKey("ladderRankedLosses")) {
                Document ladderRankedLossesDoc = result.get("ladderRankedLosses", Document.class);
                for (String ladderName : ladderRankedLossesDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderRankedLosses.put(ladder, ladderRankedLossesDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder unranked wins
            if (result.containsKey("ladderUnrankedWins")) {
                Document ladderUnrankedWinsDoc = result.get("ladderUnrankedWins", Document.class);
                for (String ladderName : ladderUnrankedWinsDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderUnrankedWins.put(ladder, ladderUnrankedWinsDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder unranked losses
            if (result.containsKey("ladderUnrankedLosses")) {
                Document ladderUnrankedLossesDoc = result.get("ladderUnrankedLosses", Document.class);
                for (String ladderName : ladderUnrankedLossesDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderUnrankedLosses.put(ladder, ladderUnrankedLossesDoc.getInteger(ladderName, 0));
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
            
            // Load per-ladder ranked win streaks
            if (result.containsKey("ladderRankedWinStreaks")) {
                Document ladderRankedWinStreaksDoc = result.get("ladderRankedWinStreaks", Document.class);
                for (String ladderName : ladderRankedWinStreaksDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderRankedWinStreaks.put(ladder, ladderRankedWinStreaksDoc.getInteger(ladderName, 0));
                    }
                }
            }
            
            // Load per-ladder best ranked win streaks
            if (result.containsKey("ladderBestRankedWinStreaks")) {
                Document ladderBestRankedWinStreaksDoc = result.get("ladderBestRankedWinStreaks", Document.class);
                for (String ladderName : ladderBestRankedWinStreaksDoc.keySet()) {
                    Ladder ladder = Practice.getInstance().getLadders().get(ladderName);
                    if (ladder != null) {
                        ladderBestRankedWinStreaks.put(ladder, ladderBestRankedWinStreaksDoc.getInteger(ladderName, 0));
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
                .append("name", Bukkit.getOfflinePlayer(uuid).getName())
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
        
        // Save per-ladder ranked wins
        Document ladderRankedWinsDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderRankedWins.entrySet()) {
            ladderRankedWinsDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderRankedWins", ladderRankedWinsDoc);
        
        // Save per-ladder ranked losses
        Document ladderRankedLossesDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderRankedLosses.entrySet()) {
            ladderRankedLossesDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderRankedLosses", ladderRankedLossesDoc);
        
        // Save per-ladder unranked wins
        Document ladderUnrankedWinsDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderUnrankedWins.entrySet()) {
            ladderUnrankedWinsDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderUnrankedWins", ladderUnrankedWinsDoc);
        
        // Save per-ladder unranked losses
        Document ladderUnrankedLossesDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderUnrankedLosses.entrySet()) {
            ladderUnrankedLossesDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderUnrankedLosses", ladderUnrankedLossesDoc);
        
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
        
        // Save per-ladder ranked win streaks
        Document ladderRankedWinStreaksDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderRankedWinStreaks.entrySet()) {
            ladderRankedWinStreaksDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderRankedWinStreaks", ladderRankedWinStreaksDoc);
        
        // Save per-ladder best ranked win streaks
        Document ladderBestRankedWinStreaksDoc = new Document();
        for (Map.Entry<Ladder, Integer> entry : ladderBestRankedWinStreaks.entrySet()) {
            ladderBestRankedWinStreaksDoc.append(entry.getKey().getName(), entry.getValue());
        }
        doc.append("ladderBestRankedWinStreaks", ladderBestRankedWinStreaksDoc);
        
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
