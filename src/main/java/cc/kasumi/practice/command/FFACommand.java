package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.match.MatchManager;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("ffa")
@CommandPermission("practice.admin")
public class FFACommand extends BaseCommand {

    private final MatchManager matchManager;

    public FFACommand(MatchManager matchManager) {
        this.matchManager = matchManager;
    }

    @Default
    @HelpCommand
    public void onFFACommand(CommandSender sender) {
        sender.sendMessage(MAIN_COLOR + "FFA Commands:");
        sender.sendMessage(MAIN_COLOR + "/ffa start <ladder> [players...] - Start an FFA match");
        sender.sendMessage(MAIN_COLOR + "/ffa startall <ladder> - Start FFA with all online players");
        sender.sendMessage(MAIN_COLOR + "/ffa forcestart <ladder> [players...] - Force start FFA with any number of players");
        sender.sendMessage(MAIN_COLOR + "/ffa forcestartall <ladder> - Force start FFA with all online players (ignores minimum)");
    }

    @Subcommand("start")
    @Syntax("<ladder> [players...]")
    public void onStartCommand(CommandSender sender, String ladderName, OnlinePlayer... players) {
        startFFAMatch(sender, ladderName, false, players);
    }

    @Subcommand("forcestart")
    @Syntax("<ladder> [players...]")
    public void onForceStartCommand(CommandSender sender, String ladderName, OnlinePlayer... players) {
        startFFAMatch(sender, ladderName, true, players);
    }

    @Subcommand("startall")
    @Syntax("<ladder>")
    public void onStartAllCommand(CommandSender sender, String ladderName) {
        startFFAMatchWithAll(sender, ladderName, false);
    }

    @Subcommand("forcestartall")
    @Syntax("<ladder>")
    public void onForceStartAllCommand(CommandSender sender, String ladderName) {
        startFFAMatchWithAll(sender, ladderName, true);
    }

    private void startFFAMatch(CommandSender sender, String ladderName, boolean force, OnlinePlayer... players) {
        Ladder ladder = Practice.getInstance().getLadders().get(ladderName.toLowerCase());

        if (ladder == null) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' not found!");
            return;
        }

        List<Player> playerList = new ArrayList<>();

        if (players.length == 0) {
            if (!force) {
                sender.sendMessage(ERROR_COLOR + "You must specify at least 3 players for an FFA match!");
                return;
            } else {
                sender.sendMessage(ERROR_COLOR + "You must specify at least 1 player for a forced FFA match!");
                return;
            }
        }

        for (OnlinePlayer onlinePlayer : players) {
            Player player = onlinePlayer.getPlayer();
            PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

            if (practicePlayer.isInMatch()) {
                sender.sendMessage(ERROR_COLOR + player.getName() + " is already in a match!");
                continue;
            }

            if (practicePlayer.isBusy()) {
                sender.sendMessage(ERROR_COLOR + player.getName() + " is busy (" +
                        practicePlayer.getPlayerState().toString().toLowerCase() + ")!");
                continue;
            }

            playerList.add(player);
        }

        if (!force && playerList.size() < 3) {
            sender.sendMessage(ERROR_COLOR + "Need at least 3 available players for FFA!");
            return;
        }

        if (playerList.size() < 1) {
            sender.sendMessage(ERROR_COLOR + "No available players found!");
            return;
        }

        try {
            matchManager.createFFAMatch(playerList, ladder,
                    Practice.getInstance().getArenaManager().getAvailableArena(), false);

            String forceText = force ? " (forced)" : "";
            sender.sendMessage(MAIN_COLOR + "Started FFA match" + forceText + " with " + SEC_COLOR + playerList.size() +
                    MAIN_COLOR + " players in " + SEC_COLOR + ladder.getDisplayName());
        } catch (Exception e) {
            sender.sendMessage(ERROR_COLOR + "Failed to create FFA match: " + e.getMessage());
        }
    }

    private void startFFAMatchWithAll(CommandSender sender, String ladderName, boolean force) {
        Ladder ladder = Practice.getInstance().getLadders().get(ladderName.toLowerCase());

        if (ladder == null) {
            sender.sendMessage(ERROR_COLOR + "Ladder '" + ladderName + "' not found!");
            return;
        }

        List<Player> availablePlayers = new ArrayList<>();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

            if (practicePlayer != null && practicePlayer.getPlayerState() == PlayerState.LOBBY) {
                availablePlayers.add(player);
            }
        }

        if (!force && availablePlayers.size() < 3) {
            sender.sendMessage(ERROR_COLOR + "Need at least 3 players in lobby for FFA! (Found: " +
                    availablePlayers.size() + ")");
            return;
        }

        if (availablePlayers.size() < 1) {
            sender.sendMessage(ERROR_COLOR + "No players in lobby found!");
            return;
        }

        try {
            matchManager.createFFAMatch(availablePlayers, ladder,
                    Practice.getInstance().getArenaManager().getAvailableArena(), false);

            String forceText = force ? " (forced)" : "";
            sender.sendMessage(MAIN_COLOR + "Started FFA match" + forceText + " with " + SEC_COLOR + availablePlayers.size() +
                    MAIN_COLOR + " players in " + SEC_COLOR + ladder.getDisplayName());
        } catch (Exception e) {
            sender.sendMessage(ERROR_COLOR + "Failed to create FFA match: " + e.getMessage());
        }
    }
}
