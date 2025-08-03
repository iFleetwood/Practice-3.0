package cc.kasumi.practice.game.match;

import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.commons.util.PlayerUtil;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.cache.CacheManager;
import cc.kasumi.practice.game.match.cache.CachedInventory;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.MatchTeam;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import cc.kasumi.practice.util.SpectatorUtil;
import cc.kasumi.practice.vanish.VanishUtil;
import com.comphenix.protocol.wrappers.Pair;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;
import static cc.kasumi.practice.PracticeConfiguration.SEC_COLOR;
import cc.kasumi.practice.player.PlayerElo;

@Getter
public abstract class Match {

    protected final MatchManager matchManager = Practice.getInstance().getMatchManager();
    protected final CacheManager cacheManager = Practice.getInstance().getCacheManager();

    // Abstract methods that subclasses must implement
    public abstract Set<MatchTeam> getTeams();
    public abstract void setupMatch();
    public abstract String getMatchStartMessage();
    public abstract String getCountdownMessage(int seconds);
    public abstract void handleMatchEnd();

    protected final Set<UUID> spectators = new HashSet<>();
    protected final UUID identifier;
    protected final MatchType matchType;
    protected final Ladder ladder;
    protected final Arena arena;
    protected final boolean ranked;
    protected long timestamp;

    @Setter
    protected MatchState matchState = MatchState.STARTING;

    protected Match(MatchType matchType, Ladder ladder, Arena arena, boolean ranked) {
        this.identifier = UUID.randomUUID();
        this.matchType = matchType;
        this.ladder = ladder;
        this.arena = arena;
        this.ranked = ranked;

        // Don't call setupMatch() here - let subclasses call it when they're ready
    }

    // Common method implementations that can be shared
    public void disconnected(Player player) {
        Pair<MatchTeam, MatchPlayer> pair = getMatchPairByUUID(player.getUniqueId());
        if (pair == null) return;

        MatchPlayer matchPlayer = pair.getSecond();
        matchPlayer.setDisconnected(true);

        if (!matchPlayer.isDead()) {
            handleDeath(player, player.getLocation(), true);
        }
    }

    public void handleDeath(Player player, Location location, boolean hidePlayer) {
        Pair<MatchTeam, MatchPlayer> pair = getMatchPairByUUID(player.getUniqueId());
        if (pair == null) return;

        MatchTeam matchTeam = pair.getFirst();
        MatchPlayer matchPlayer = pair.getSecond();

        matchPlayer.setDead(true);
        
        // Record death in statistics
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        if (practicePlayer != null) {
            practicePlayer.recordDeath();
        }
        
        cacheManager.cachePlayerInventory(player, ladder.getInventoryType(), true);
        PlayerUtil.resetPlayer(player);

        if (hidePlayer) {
            hidePlayerFromOthers(player);
            startSpectating(player);
        }

        onPlayerDeath(player, matchTeam, matchPlayer);
    }

    // Template method for handling player death - can be overridden
    protected void onPlayerDeath(Player player, MatchTeam playerTeam, MatchPlayer matchPlayer) {
        checkForMatchEnd();
    }

    // Common countdown implementation
    protected void countdown() {
        new BukkitRunnable() {
            int i = 5;

            @Override
            public void run() {
                if (matchState == MatchState.CANCELED || matchState == MatchState.ENDED) {
                    cancel();
                    return;
                }

                if (this.i <= 0) {
                    startMatch();
                    sendMessage(getMatchStartMessage());
                    cancel();
                    return;
                }

                sendMessage(getCountdownMessage(i));
                i--;
            }
        }.runTaskTimer(Practice.getInstance(), 0, 20);
    }

    protected void startMatch() {
        this.timestamp = System.currentTimeMillis();
        this.matchState = MatchState.ONGOING;
    }

    protected void checkForMatchEnd() {
        List<MatchTeam> aliveTeams = getAliveTeams();

        if (shouldEndMatch(aliveTeams)) {
            matchState = MatchState.ENDING;
            handleMatchEnd();
        }
    }

    // Template method that can be overridden
    protected boolean shouldEndMatch(List<MatchTeam> aliveTeams) {
        return aliveTeams.size() <= 1;
    }

    protected List<MatchTeam> getAliveTeams() {
        return getTeams().stream()
                .filter(team -> team.getAlive() > 0)
                .collect(Collectors.toList());
    }

    // Common player setup
    protected void setupPlayer(Player player, Location location) {
        UUID uuid = player.getUniqueId();

        // Use enhanced vanish system
        VanishUtil.hideAllPlayers(player);
        PlayerUtil.resetPlayer(player);

        player.teleport(location);
        setPracticePlayerSettings(PracticePlayer.getPracticePlayer(uuid));
        giveKit(player);

        player.spigot().setCollidesWithEntities(true);
        player.setCanPickupItems(true);
    }

    protected void setPracticePlayerSettings(PracticePlayer practicePlayer) {
        practicePlayer.setCurrentMatch(this);
        practicePlayer.setPlayerState(PlayerState.PLAYING);
    }

    protected void giveKit(Player player) {
        PlayerInv kit = ladder.getDefaultKit();
        PlayerInventory inventory = player.getInventory();
        inventory.setContents(kit.getContents());
        inventory.setArmorContents(kit.getArmorContents());
        player.updateInventory();
    }

    // Common spectator methods
    protected void startSpectating(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        
        // Set player state to spectating
        practicePlayer.setPlayerState(PlayerState.SPECTATING);
        
        // Add to match spectators
        spectators.add(player.getUniqueId());
        
        // Set creative mode instead of spectator mode so they can interact with items
        player.setGameMode(GameMode.CREATIVE);
        
        // Enable flying
        player.setAllowFlight(true);
        player.setFlying(true);
        
        // Give spectator items
        SpectatorUtil.giveSpectatorItems(player);
        
        // Set up vanish for spectator
        VanishUtil.setupSpectatorVanish(player, this);
    }

    protected void stopSpectating(Player player) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
        
        // Reset player state
        practicePlayer.setPlayerState(PlayerState.LOBBY);
        
        // Remove from match spectators
        spectators.remove(player.getUniqueId());
        
        // Reset gamemode back to survival from creative
        player.setGameMode(GameMode.SURVIVAL);
        
        // Disable flying
        player.setAllowFlight(false);
        player.setFlying(false);
        
        // Remove spectator items and give lobby items
        SpectatorUtil.removeSpectatorItems(player);
    }

    // Common messaging
    protected void sendMessage(String message) {
        getBukkitPlayers().forEach(player -> player.sendMessage(message));
    }

    // Common player management
    public Set<Player> getBukkitPlayers() {
        Set<Player> players = new HashSet<>();
        for (MatchTeam team : getTeams()) {
            players.addAll(team.getBukkitPlayers());
        }
        return players;
    }

    protected void showPlayersToEachOther(Collection<Player> players) {
        // Use the enhanced vanish system for better tracking
        VanishUtil.showMatchPlayers(this);
    }

    protected void hidePlayersFromEachOther(Collection<Player> players) {
        for (Player player : players) {
            for (Player otherPlayer : players) {
                if (!player.equals(otherPlayer)) {
                    player.hidePlayer(otherPlayer);
                }
            }
        }
    }

    protected void hidePlayerFromOthers(Player player) {
        VanishUtil.hidePlayerFromAll(player);
    }

    protected Pair<MatchTeam, MatchPlayer> getMatchPairByUUID(UUID uuid) {
        for (MatchTeam team : getTeams()) {
            for (MatchPlayer matchPlayer : team.getPlayers()) {
                if (matchPlayer.getUuid().equals(uuid)) {
                    return new Pair<>(team, matchPlayer);
                }
            }
        }
        return null;
    }

    // Common match ending
    protected void endMatchForPlayer(Player player, boolean winner) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        String message = getEndMessage(player, winner);
        player.sendMessage(message);

        // Update ELO rating for ranked matches
        if (ranked && practicePlayer != null) {
            updatePlayerRating(practicePlayer, player, winner);
        }
        
        // Record match result in statistics
        if (practicePlayer != null) {
            if (winner) {
                practicePlayer.recordWin(ladder, ranked);
            } else {
                practicePlayer.recordLoss(ladder, ranked);
            }
        }

        PlayerUtil.resetPlayer(player);
        player.teleport(player.getWorld().getSpawnLocation());
        stopSpectating(player);
        player.getInventory().setContents(GameUtil.getLobbyContents());
        VanishUtil.showAllPlayers(player);

        practicePlayer.setPlayerState(PlayerState.LOBBY);
        practicePlayer.setCurrentMatch(null);
    }

    // Template method for end messages - can be overridden
    protected String getEndMessage(Player player, boolean winner) {
        if (winner) {
            return MAIN_COLOR + "Congratulations! You won the match!";
        } else {
            return MAIN_COLOR + "You lost the match!";
        }
    }

    protected void cacheInventoriesForAlivePlayers(List<MatchPlayer> matchPlayers) {
        for (MatchPlayer matchPlayer : matchPlayers) {
            if (matchPlayer.isDead()) continue;

            Player player = matchPlayer.getPlayer();
            if (player != null) {
                cacheManager.cachePlayerInventory(player, ladder.getInventoryType(), false);
            }
        }
    }

    protected Pair<String, TextComponent> createAfterMatchMessage(List<MatchPlayer> winners, List<MatchPlayer> allPlayers) {
        String splitter = ", ";
        StringBuilder winnersBuilder = new StringBuilder().append(ChatColor.YELLOW);
        TextComponent inventoriesComponent = new TextComponent("Inventories (click to view): ");
        inventoriesComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);

        // Build winners message
        if (winners.size() == 1) {
            winnersBuilder.append("Winner: ").append(winners.get(0).getName());
        } else if (winners.size() > 1) {
            winnersBuilder.append("Winners: ");
            for (int i = 0; i < winners.size(); i++) {
                winnersBuilder.append(winners.get(i).getName());
                if (i < winners.size() - 1) {
                    winnersBuilder.append(", ");
                }
            }
        }

        // Add clickable inventories
        for (int i = 0; i < allPlayers.size(); i++) {
            MatchPlayer player = allPlayers.get(i);
            TextComponent clickableComponent = CachedInventory.getInventoryMessage(player.getUuid(), player.getName());

            inventoriesComponent.addExtra(clickableComponent);

            if (i < allPlayers.size() - 1) {
                inventoriesComponent.addExtra(splitter);
            }
        }

        return new Pair<>(winnersBuilder.toString(), inventoriesComponent);
    }

    protected void finishMatch() {
        // Handle any remaining external spectators
        for (UUID spectatorUUID : new HashSet<>(spectators)) {
            Player spectator = org.bukkit.Bukkit.getPlayer(spectatorUUID);
            if (spectator != null && spectator.isOnline()) {
                PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(spectatorUUID);
                if (practicePlayer != null && practicePlayer.getPlayerState() == PlayerState.SPECTATING) {
                    // Reset spectator
                    stopSpectating(spectator);
                    spectator.teleport(spectator.getWorld().getSpawnLocation());
                    spectator.getInventory().setContents(GameUtil.getLobbyContents());
                    VanishUtil.showAllPlayers(spectator);
                    practicePlayer.setPlayerState(PlayerState.LOBBY);
                    practicePlayer.setSpectatingMatch(null);
                    spectator.sendMessage("§cThe match you were spectating has ended.");
                }
            }
        }
        
        matchManager.getMatches().remove(this);
    }

    /**
     * Update player's ELO rating for the ladder after a ranked match
     */
    private void updatePlayerRating(PracticePlayer practicePlayer, Player player, boolean winner) {
        PlayerElo currentElo = practicePlayer.getLadderElo(ladder);
        
        // Calculate opponent's average rating
        int opponentRating = calculateOpponentAverageRating(player);
        
        // Calculate new rating based on match result
        double score = winner ? 1.0 : 0.0; // 1.0 for win, 0.0 for loss
        int newRating = currentElo.getNewRating(opponentRating, score);
        
        // Update the rating
        practicePlayer.setLadderElo(ladder, new PlayerElo(newRating));
        
        // Save the updated rating
        practicePlayer.save(true);
        
        // Notify player of rating change
        int ratingChange = newRating - currentElo.getRating();
        String changeColor = ratingChange >= 0 ? "§a+" : "§c";
        player.sendMessage("§7ELO Rating: §f" + currentElo.getRating() + " §7→ §f" + newRating + " §7(" + changeColor + ratingChange + "§7)");
    }
    
    /**
     * Calculate the average rating of all opponents for this player
     */
    private int calculateOpponentAverageRating(Player player) {
        List<Integer> opponentRatings = new ArrayList<>();
        
        // Get all players from all teams
        for (MatchTeam team : getTeams()) {
            for (MatchPlayer matchPlayer : team.getPlayers()) {
                Player otherPlayer = matchPlayer.getPlayer();
                if (otherPlayer != null && !otherPlayer.equals(player)) {
                    PracticePlayer otherPracticePlayer = PracticePlayer.getPracticePlayer(otherPlayer.getUniqueId());
                    if (otherPracticePlayer != null) {
                        opponentRatings.add(otherPracticePlayer.getLadderElo(ladder).getRating());
                    }
                }
            }
        }
        
        if (opponentRatings.isEmpty()) {
            return 1000; // Default rating if no opponents found
        }
        
        return opponentRatings.stream().mapToInt(Integer::intValue).sum() / opponentRatings.size();
    }
}