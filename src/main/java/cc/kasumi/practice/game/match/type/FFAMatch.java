package cc.kasumi.practice.game.match.type;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.match.MatchState;
import cc.kasumi.practice.game.match.MatchType;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.MatchTeam;
import cc.kasumi.practice.game.match.team.SoloTeam;
import com.comphenix.protocol.wrappers.Pair;
import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;
import static cc.kasumi.practice.PracticeConfiguration.SEC_COLOR;

@Getter
public class FFAMatch extends Match {

    private final Set<MatchTeam> teams = new HashSet<>();
    private final List<Location> spawnLocations = new ArrayList<>();

    public FFAMatch(MatchType matchType, Ladder ladder, Arena arena, boolean ranked, List<MatchPlayer> players) {
        super(matchType, ladder, arena, ranked);

        // Create individual teams for each player (FFA = everyone vs everyone)
        for (MatchPlayer matchPlayer : players) {
            teams.add(new SoloTeam(matchPlayer));
        }

        generateSpawnLocations();

        // Now that teams and spawn locations are set, we can setup the match
        setupMatch();
    }

    @Override
    public Set<MatchTeam> getTeams() {
        return teams;
    }

    @Override
    public void setupMatch() {
        List<Player> allPlayers = getBukkitPlayers().stream().collect(Collectors.toList());

        // Setup each player at their respective spawn location
        int index = 0;
        for (MatchTeam team : teams) {
            for (Player player : team.getBukkitPlayers()) {
                Location spawnLocation = spawnLocations.get(index % spawnLocations.size());
                setupPlayer(player, spawnLocation);
                index++;
            }
        }

        // Show all players to each other
        showPlayersToEachOther(allPlayers);
        countdown();
    }

    @Override
    public String getMatchStartMessage() {
        if (teams.size() == 1) {
            return MAIN_COLOR + "The FFA match has started! (Solo practice mode)";
        } else if (teams.size() == 2) {
            return MAIN_COLOR + "The FFA match has started! (1v1 mode)";
        } else {
            return MAIN_COLOR + "The FFA match has started! Last player standing wins!";
        }
    }

    @Override
    public String getCountdownMessage(int seconds) {
        String secondText = seconds == 1 ? " second..." : " seconds...";
        return SEC_COLOR + "The FFA match is starting in " + MAIN_COLOR + seconds + SEC_COLOR + secondText;
    }

    @Override
    protected void onPlayerDeath(Player player, MatchTeam playerTeam, MatchPlayer matchPlayer) {
        // Announce elimination (only if more than 1 player total)
        if (teams.size() > 1) {
            String playerName = matchPlayer.getName();
            sendMessage(SEC_COLOR + playerName + MAIN_COLOR + " has been eliminated!");
        }

        super.onPlayerDeath(player, playerTeam, matchPlayer);
    }

    @Override
    protected void checkForMatchEnd() {
        List<MatchTeam> aliveTeams = getAliveTeams();

        if (shouldEndMatch(aliveTeams)) {
            matchState = MatchState.ENDING;
            handleMatchEnd();
        } else {
            // Announce remaining players (only if more than 1 player total)
            if (teams.size() > 1) {
                sendMessage(MAIN_COLOR + "" + aliveTeams.size() + SEC_COLOR + " players remaining!");
            }
        }
    }

    @Override
    public void handleMatchEnd() {
        List<MatchTeam> aliveTeams = getAliveTeams();

        if (aliveTeams.size() == 1) {
            // We have a winner
            MatchTeam winner = aliveTeams.get(0);
            cacheInventoriesForAlivePlayers(winner.getPlayers());

            new BukkitRunnable() {
                @Override
                public void run() {
                    endMatch(winner);
                }
            }.runTaskLater(Practice.getInstance(), 2 * 20L);
        } else {
            // Draw case (everyone died)
            handleDraw();
        }
    }

    private void endMatch(MatchTeam winner) {
        matchState = MatchState.ENDED;

        List<MatchPlayer> allPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .collect(Collectors.toList());

        Pair<String, TextComponent> inventories = createAfterMatchMessage(winner.getPlayers(), allPlayers);

        // Handle winner
        for (MatchPlayer matchPlayerWinner : winner.getPlayers()) {
            if (matchPlayerWinner.isDisconnected()) continue;

            Player playerWinner = matchPlayerWinner.getPlayer();
            if (playerWinner == null) continue;

            endMatchForPlayer(playerWinner, true);
            playerWinner.sendMessage(inventories.getFirst());
            playerWinner.spigot().sendMessage(inventories.getSecond());
        }

        // Handle losers
        List<MatchTeam> losers = teams.stream()
                .filter(team -> team != winner)
                .collect(Collectors.toList());

        for (MatchTeam loserTeam : losers) {
            for (MatchPlayer matchPlayerLoser : loserTeam.getPlayers()) {
                if (matchPlayerLoser.isDisconnected()) continue;

                Player playerLoser = matchPlayerLoser.getPlayer();
                if (playerLoser == null) continue;

                endMatchForPlayer(playerLoser, false);
                playerLoser.sendMessage(inventories.getFirst());
                playerLoser.spigot().sendMessage(inventories.getSecond());
            }
        }

        finishMatch();
    }

    private void handleDraw() {
        matchState = MatchState.ENDED;
        sendMessage(MAIN_COLOR + "The FFA match ended in a draw!");

        List<MatchPlayer> allPlayers = teams.stream()
                .flatMap(team -> team.getPlayers().stream())
                .collect(Collectors.toList());

        Pair<String, TextComponent> inventories = createAfterMatchMessage(allPlayers, allPlayers);

        for (MatchPlayer matchPlayer : allPlayers) {
            if (matchPlayer.isDisconnected()) continue;

            Player player = matchPlayer.getPlayer();
            if (player == null) continue;

            endMatchForPlayer(player, false);
            player.sendMessage(inventories.getFirst());
            player.spigot().sendMessage(inventories.getSecond());
        }

        finishMatch();
    }

    @Override
    protected String getEndMessage(Player player, boolean winner) {
        if (winner) {
            if (teams.size() == 1) {
                return MAIN_COLOR + "Practice session completed!";
            } else {
                return MAIN_COLOR + "Congratulations! You won the FFA match!";
            }
        } else {
            if (teams.size() == 1) {
                return MAIN_COLOR + "Practice session ended!";
            } else {
                return MAIN_COLOR + "You were eliminated from the FFA match!";
            }
        }
    }

    private void generateSpawnLocations() {
        Location centerA = arena.getSpawnLocationA();
        Location centerB = arena.getSpawnLocationB();

        // Calculate center point between the two spawn locations
        double centerX = (centerA.getX() + centerB.getX()) / 2;
        double centerY = Math.max(centerA.getY(), centerB.getY());
        double centerZ = (centerA.getZ() + centerB.getZ()) / 2;

        Location center = new Location(centerA.getWorld(), centerX, centerY, centerZ);

        // Generate spawn locations in a circle around the center
        int playerCount = teams.size();
        double radius = 10.0; // Adjust radius as needed

        for (int i = 0; i < playerCount; i++) {
            double angle = 2 * Math.PI * i / playerCount;
            double x = center.getX() + radius * Math.cos(angle);
            double z = center.getZ() + radius * Math.sin(angle);

            Location spawnLocation = new Location(center.getWorld(), x, centerY, z);
            spawnLocation.setYaw((float) (angle * 180 / Math.PI + 180)); // Face inward
            spawnLocations.add(spawnLocation);
        }
    }
}