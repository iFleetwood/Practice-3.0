package cc.kasumi.practice.command;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.QueuePlayer;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.vanish.VanishManager;
import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static cc.kasumi.practice.PracticeConfiguration.*;

@CommandAlias("qdebug|queuedebug")
@CommandPermission("practice.admin")
public class QueueDebugCommand extends BaseCommand {

    @Default
    @HelpCommand
    public void onQueueDebugCommand(CommandSender sender) {
        sender.sendMessage(MAIN_COLOR + "Queue Debug Commands:");
        sender.sendMessage(MAIN_COLOR + "/qdebug list - List all queues and their status");
        sender.sendMessage(MAIN_COLOR + "/qdebug players - List all players and their states");
        sender.sendMessage(MAIN_COLOR + "/qdebug clear <queue> - Clear a specific queue");
        sender.sendMessage(MAIN_COLOR + "/qdebug clearall - Clear all queues");
    }

    @Subcommand("list")
    public void onListCommand(CommandSender sender) {
        sender.sendMessage(MAIN_COLOR + "=== Queue Status ===");

        for (Queue queue : Practice.getInstance().getQueueManager().getQueues().values()) {
            sender.sendMessage(SEC_COLOR + "Queue: " + MAIN_COLOR + queue.getName());
            sender.sendMessage("  Type: " + queue.getType());
            sender.sendMessage("  Ladder: " + queue.getLadder().getName());
            sender.sendMessage("  Ranked: " + queue.isRanked());
            sender.sendMessage("  Players: " + queue.getPlayers().size());

            for (QueuePlayer qp : queue.getPlayers()) {
                Player p = qp.getPlayer();
                String playerName = p != null ? p.getName() : "OFFLINE";
                sender.sendMessage("    - " + playerName + " (UUID: " + qp.getUuid() + ")");
            }
            sender.sendMessage("");
        }
    }

    @Subcommand("players")
    public void onPlayersCommand(CommandSender sender) {
        sender.sendMessage(MAIN_COLOR + "=== Player States ===");

        for (Player player : Bukkit.getOnlinePlayers()) {
            PracticePlayer pp = PracticePlayer.getPracticePlayer(player.getUniqueId());
            if (pp == null) {
                sender.sendMessage(ERROR_COLOR + player.getName() + " - NO PRACTICE PLAYER DATA");
                continue;
            }

            sender.sendMessage(SEC_COLOR + player.getName() + ":");
            sender.sendMessage("  State: " + pp.getPlayerState());
            sender.sendMessage("  In Match: " + pp.isInMatch());
            sender.sendMessage("  Current Queue: " + (pp.getCurrentQueue() != null ? pp.getCurrentQueue().getName() : "None"));
            sender.sendMessage("  Current Match: " + (pp.getCurrentMatch() != null ? pp.getCurrentMatch().getIdentifier() : "None"));
        }
    }

    @Subcommand("clear")
    @Syntax("<queue>")
    public void onClearCommand(CommandSender sender, String queueName) {
        Queue queue = Practice.getInstance().getQueueManager().getQueues().get(queueName);

        if (queue == null) {
            sender.sendMessage(ERROR_COLOR + "Queue '" + queueName + "' not found!");
            return;
        }

        int playerCount = queue.getPlayers().size();

        // Reset player states
        for (QueuePlayer qp : queue.getPlayers()) {
            Player p = qp.getPlayer();
            if (p != null) {
                PracticePlayer pp = PracticePlayer.getPracticePlayer(p.getUniqueId());
                if (pp != null) {
                    pp.setCurrentQueue(null);
                    pp.setPlayerState(cc.kasumi.practice.player.PlayerState.LOBBY);
                }
            }
        }

        queue.getPlayers().clear();
        sender.sendMessage(MAIN_COLOR + "Cleared " + playerCount + " players from queue '" + queueName + "'");
    }

    @Subcommand("vanish")
    public void onVanishDebugCommand(CommandSender sender) {
        VanishManager vanishManager = Practice.getInstance().getVanishManager();

        sender.sendMessage(MAIN_COLOR + "=== Vanish Debug ===");
        sender.sendMessage("Sources tracked: " + vanishManager.getSources().size());
        sender.sendMessage("Particles tracked: " + vanishManager.getParticles().size());
        sender.sendMessage("Vanished players: " + vanishManager.getVanishedPlayers().size());

        sender.sendMessage(MAIN_COLOR + "Can-See Relationships:");
        vanishManager.getCanSeeMap().forEach((viewer, targets) -> {
            Player viewerPlayer = Bukkit.getPlayer(viewer);
            String viewerName = viewerPlayer != null ? viewerPlayer.getName() : "OFFLINE";
            sender.sendMessage("  " + viewerName + " can see " + targets.size() + " players");
        });
    }

    @Subcommand("vanish clear")
    public void onVanishClearCommand(CommandSender sender) {
        VanishManager vanishManager = Practice.getInstance().getVanishManager();

        vanishManager.getSources().clear();
        vanishManager.getParticles().clear();
        vanishManager.getCanSeeMap().clear();
        vanishManager.getVanishedPlayers().clear();

        sender.sendMessage(MAIN_COLOR + "Cleared all vanish data!");
    }
}