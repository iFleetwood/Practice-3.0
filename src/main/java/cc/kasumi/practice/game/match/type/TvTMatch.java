package cc.kasumi.practice.game.match.type;

import cc.kasumi.commons.util.PlayerInv;
import cc.kasumi.commons.util.PlayerUtil;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.arena.Arena;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.Match;
import cc.kasumi.practice.game.match.MatchState;
import cc.kasumi.practice.game.match.MatchType;
import cc.kasumi.practice.game.match.cache.CachedInventory;
import cc.kasumi.practice.game.match.player.MatchPlayer;
import cc.kasumi.practice.game.match.team.MatchTeam;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import cc.kasumi.practice.vanish.VanishUtil;
import com.comphenix.protocol.wrappers.Pair;
import lombok.Getter;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static cc.kasumi.practice.PracticeConfiguration.MAIN_COLOR;
import static cc.kasumi.practice.PracticeConfiguration.SEC_COLOR;

@Getter
public class TvTMatch extends Match {

    private final MatchTeam matchTeamA;
    private final MatchTeam matchTeamB;

    public TvTMatch(MatchType matchType, Ladder ladder, Arena arena, boolean ranked, MatchTeam matchTeamA, MatchTeam matchTeamB) {
        super(matchType, ladder, arena, ranked);

        this.matchTeamA = matchTeamA;
        this.matchTeamB = matchTeamB;

        setupMatch();
    }

    private void setupMatch() {
        List<Player> matchTeamAPlayers = matchTeamA.getBukkitPlayers();
        List<Player> matchTeamBPlayers = matchTeamB.getBukkitPlayers();

        for (Player playerA : matchTeamAPlayers) {
            setupPlayer(playerA, arena.getSpawnLocationA());
        }

        for (Player playerB : matchTeamBPlayers) {
            setupPlayer(playerB, arena.getSpawnLocationB());
        }

        showPlayersToTeams(matchTeamAPlayers, matchTeamBPlayers);

        countdown();
    }

    public void countdown() {
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
                    sendMessage(MAIN_COLOR + "The match has started!");

                    cancel();
                    return;
                }

                if (this.i != 1) {
                    sendMessage(SEC_COLOR + "The match is starting in " + MAIN_COLOR + this.i + SEC_COLOR + " seconds...");
                } else {
                    sendMessage(SEC_COLOR + "The match is starting in " + MAIN_COLOR + this.i + SEC_COLOR + " second...");
                }

                i--;
            }
        }.runTaskTimer(Practice.getInstance(), 0, 20);
    }

    public void startMatch() {
        this.timestamp = System.currentTimeMillis();
        this.matchState = MatchState.ONGOING;
    }

    public void sendMessage(String message) {
        getBukkitPlayers().forEach(player -> player.sendMessage(message));
    }

    public void showPlayersToTeams(List<Player> matchTeamAPlayers, List<Player> matchTeamBPlayers) {
        for (Player playerA : matchTeamAPlayers) {
            for (Player playerB : matchTeamBPlayers) {
                playerA.showPlayer(playerB);
                playerB.showPlayer(playerA);
            }
        }
    }

    public void hidePlayersFromTeams(List<Player> matchTeamAPlayers, List<Player> matchTeamBPlayers) {
        for (Player playerA : matchTeamAPlayers) {
            for (Player playerB : matchTeamBPlayers) {
                playerA.hidePlayer(playerB);
                playerB.hidePlayer(playerA);
            }
        }
    }

    private void hidePlayerFromTeams(Player player) {
        for (Player bukkitMatchPlayer : getBukkitPlayers()) {
            if (bukkitMatchPlayer == null || bukkitMatchPlayer.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }

            bukkitMatchPlayer.hidePlayer(player);
        }
    }

    public void setupPlayer(Player player, Location location) {
        UUID uuid = player.getUniqueId();

        VanishUtil.hideAllPlayers(player);
        PlayerUtil.resetPlayer(player);

        player.teleport(location);

        setPracticePlayerSettings(PracticePlayer.getPracticePlayer(uuid));

        giveKit(player);
        player.spigot().setCollidesWithEntities(true);
        player.setCanPickupItems(true);
    }

    public void setPracticePlayerSettings(PracticePlayer practicePlayer) {
        practicePlayer.setCurrentMatch(this);
        practicePlayer.setPlayerState(PlayerState.PLAYING);
    }

    private void giveKit(Player player) {
        PlayerInv kit = ladder.getDefaultKit();
        PlayerInventory inventory = player.getInventory();
        inventory.setContents(kit.getContents());
        inventory.setArmorContents(kit.getArmorContents());
        player.updateInventory();
    }

    public Set<Player> getBukkitPlayers() {
        Set<Player> players = new HashSet<>();

        players.addAll(matchTeamA.getBukkitPlayers());
        players.addAll(matchTeamB.getBukkitPlayers());

        return players;
    }

    public Pair<MatchTeam, MatchPlayer> getMatchPairByUUID(UUID uuid) {
        Set<MatchTeam> teams = getTeams();

        for (MatchTeam team : teams) {
            for (MatchPlayer matchPlayer : team.getPlayers()) {
                if (!matchPlayer.getUuid().equals(uuid)) {
                    continue;
                }

                return new Pair<>(team, matchPlayer);
            }
        }

        return null;
    }

    private void startSpectating(Player player) {
        player.setAllowFlight(true);
        player.setFlying(true);
    }

    private void stopSpectating(Player player) {
        player.setAllowFlight(false);
        player.setFlying(false);
    }

    @Override
    public Set<MatchTeam> getTeams() {
        Set<MatchTeam> teams = new HashSet<>();

        teams.add(matchTeamA);
        teams.add(matchTeamB);

        return teams;
    }

    public void endMatch(MatchTeam winner, MatchTeam loser) {
        matchState = MatchState.ENDED;

        Pair<String, TextComponent> inventories = getAfterMatchMessage(winner.getPlayers(), loser.getPlayers());

        for (MatchPlayer matchPlayerWinner : winner.getPlayers()) {
            if (matchPlayerWinner.isDisconnected()) continue;

            Player playerWinner = matchPlayerWinner.getPlayer();

            if (playerWinner == null) continue;

            endMatchForPlayer(playerWinner, true);

            playerWinner.sendMessage(inventories.getFirst());
            playerWinner.spigot().sendMessage(inventories.getSecond());
        }

        for (MatchPlayer matchPlayerLoser : loser.getPlayers()) {
            if (matchPlayerLoser.isDisconnected()) continue;

            Player playerLoser = matchPlayerLoser.getPlayer();

            if (playerLoser == null) continue;

            endMatchForPlayer(playerLoser, false);

            playerLoser.sendMessage(inventories.getFirst());
            playerLoser.spigot().sendMessage(inventories.getSecond());
        }

        matchManager.getMatches().remove(this);
    }

    private void endMatchForPlayer(Player player, boolean winner) {
        PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

        if (winner) {
            player.sendMessage(MAIN_COLOR + "Congratulations you won!");
        } else {
            player.sendMessage(MAIN_COLOR + "You lost!");
        }

        PlayerUtil.resetPlayer(player);
        player.teleport(player.getWorld().getSpawnLocation());
        stopSpectating(player);
        player.getInventory().setContents(GameUtil.getLobbyContents());
        VanishUtil.showAllPlayers(player);

        practicePlayer.setPlayerState(PlayerState.LOBBY);
        practicePlayer.setCurrentMatch(null);
    }

    public MatchTeam getOtherTeam(MatchTeam team) {
        if (team == matchTeamA) {
            return matchTeamB;
        } else {
            return matchTeamA;
        }
    }

    @Override
    public void disconnected(Player player) {
        Pair<MatchTeam, MatchPlayer> pair = getMatchPairByUUID(player.getUniqueId());
        MatchPlayer matchPlayer = pair.getSecond();

        matchPlayer.setDisconnected(true);

        if (!matchPlayer.isDead()) {
            handleDeath(player, player.getLocation(), true);
        }
    }

    @Override
    public void handleDeath(Player player, Location location, boolean hidePlayer) {
        Pair<MatchTeam, MatchPlayer> pair = getMatchPairByUUID(player.getUniqueId());
        MatchTeam matchTeam = pair.getFirst();
        MatchPlayer matchPlayer = pair.getSecond();

        matchPlayer.setDead(true);
        cacheManager.cachePlayerInventory(player, ladder.getInventoryType(), true);
        PlayerUtil.resetPlayer(player);

        if (hidePlayer) {
            hidePlayerFromTeams(player);
            startSpectating(player);
        }

        if (matchTeam.getAlive() <= 0) {
            matchState = MatchState.ENDING;

            MatchTeam otherTeam = getOtherTeam(matchTeam);
            cacheInventoriesForAlivePlayers(otherTeam.getPlayers());

            new BukkitRunnable() {
                @Override
                public void run() {

                    endMatch(otherTeam, matchTeam);
                }
            }.runTaskLater(Practice.getInstance(), 2*20L);
        }
    }

    private void cacheInventoriesForAlivePlayers(List<MatchPlayer> matchPlayers) {
        for (MatchPlayer matchplayer : matchPlayers) {
            if (matchplayer.isDead()) continue;

            Player player = matchplayer.getPlayer();

            if (player != null) {
                cacheManager.cachePlayerInventory(player, ladder.getInventoryType(), false);
            }
        }
    }

    private Pair<String, TextComponent> getAfterMatchMessage(List<MatchPlayer> winnerPlayers, List<MatchPlayer> loserPlayers) {
        String splitter = ", ";
        StringBuilder winnersBuilder = new StringBuilder().append(ChatColor.YELLOW).append("Winners: ");
        TextComponent inventoriesComponent = new TextComponent("Inventories (click to view): ");
        inventoriesComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);

        int i = 0;
        for (MatchPlayer winnerPlayer : winnerPlayers) {
            String name = winnerPlayer.getName();
            TextComponent clickableComponent = CachedInventory.getInventoryMessage(winnerPlayer.getUuid(), name);

            if (this.ranked)

            if (i++ < winnerPlayers.size()) {
                winnersBuilder.append(winnerPlayer.getName()).append(splitter);

                continue;
            }

            winnersBuilder.append(name).append(".");

            inventoriesComponent.addExtra(clickableComponent);
            inventoriesComponent.addExtra(splitter);
        }

        i = 1;
        for (MatchPlayer loserPlayer : loserPlayers) {
            TextComponent clickableComponent = CachedInventory.getInventoryMessage(loserPlayer.getUuid(), loserPlayer.getName());

            inventoriesComponent.addExtra(clickableComponent);

            if (i++ < loserPlayers.size()) {
                inventoriesComponent.addExtra(splitter);
            }
        }

        return new Pair<>(winnersBuilder.toString(), inventoriesComponent);
    }
}
