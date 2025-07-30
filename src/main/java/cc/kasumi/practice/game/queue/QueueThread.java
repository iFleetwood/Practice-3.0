package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.match.MatchManager;
import cc.kasumi.practice.game.queue.type.FFAQueue;
import cc.kasumi.practice.game.queue.type.SoloQueue;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QueueThread extends Thread {

    private final Practice plugin;
    private final MatchManager matchManager;
    private final QueueManager queueManager;

    public QueueThread(Practice plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
        this.matchManager = plugin.getMatchManager();
    }

    @Override
    public void run() {
        while (true) {
            try {
                for (Queue queue : queueManager.getQueues().values()) {
                    try {
                        if (queue.getType() == QueueType.FFA) {
                            handleFFAQueue(queue);
                        } else if (queue.getType() == QueueType.SOLO) {
                            handleSoloQueue(queue);
                        }
                        // Add support for other queue types here if needed
                    } catch (Exception queueException) {
                        System.err.println("Error processing queue " + queue.getName() + ": " + queueException.getMessage());
                        queueException.printStackTrace();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();

                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e2) {
                    e2.printStackTrace();
                }
                continue;
            }

            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleSoloQueue(Queue queue) {
        // Cast to SoloQueue for specific functionality if needed
        SoloQueue soloQueue = (SoloQueue) queue;

        if (!soloQueue.isReadyForMatch()) {
            return;
        }

        List<QueuePlayer> queuePlayers = soloQueue.getPlayers();

        // Create a copy to avoid concurrent modification
        List<QueuePlayer> playersCopy = new ArrayList<>(queuePlayers);

        for (QueuePlayer firstQueuePlayer : playersCopy) {
            Player firstPlayer = firstQueuePlayer.getPlayer();

            if (firstPlayer == null) {
                // Remove offline player from queue
                queuePlayers.remove(firstQueuePlayer);
                continue;
            }

            for (QueuePlayer secondQueuePlayer : playersCopy) {
                if (firstQueuePlayer.equals(secondQueuePlayer)) {
                    continue;
                }

                Player secondPlayer = secondQueuePlayer.getPlayer();

                if (secondPlayer == null) {
                    // Remove offline player from queue
                    queuePlayers.remove(secondQueuePlayer);
                    continue;
                }

                // Check if players are in rating range for ranked matches
                if (queue.isRanked()) {
                    if (!firstQueuePlayer.isInRange(secondQueuePlayer.getRating()) ||
                            !secondQueuePlayer.isInRange(firstQueuePlayer.getRating())) {
                        continue;
                    }
                }

                // Found a match! Remove both players and create match
                queuePlayers.remove(firstQueuePlayer);
                queuePlayers.remove(secondQueuePlayer);

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PracticePlayer firstPracticePlayer = PracticePlayer.getPracticePlayer(firstPlayer.getUniqueId());
                        PracticePlayer secondPracticePlayer = PracticePlayer.getPracticePlayer(secondPlayer.getUniqueId());

                        if (firstPracticePlayer != null) {
                            firstPracticePlayer.setCurrentQueue(null);
                        }
                        if (secondPracticePlayer != null) {
                            secondPracticePlayer.setCurrentQueue(null);
                        }

                        try {
                            matchManager.createSoloMatch(firstPlayer, secondPlayer, queue.getLadder(),
                                    plugin.getArenaManager().getAvailableArena(), queue.isRanked());
                        } catch (Exception e) {
                            e.printStackTrace();
                            // If match creation fails, notify players
                            firstPlayer.sendMessage("§cFailed to create match. Please try again.");
                            secondPlayer.sendMessage("§cFailed to create match. Please try again.");
                        }
                    }
                }.runTask(plugin);

                return; // Exit after creating one match
            }
        }
    }

    private void handleFFAQueue(Queue queue) {
        // Cast to FFAQueue for specific functionality
        FFAQueue ffaQueue = (FFAQueue) queue;

        if (!ffaQueue.isReadyForMatch()) {
            return;
        }

        List<QueuePlayer> playersForMatch = ffaQueue.getPlayersForMatch();
        List<Player> bukkitPlayers = playersForMatch.stream()
                .map(QueuePlayer::getPlayer)
                .filter(player -> player != null)
                .collect(Collectors.toList());

        if (bukkitPlayers.size() < ffaQueue.getMinPlayers()) {
            return; // Not enough valid players
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Clear queue assignments
                for (Player player : bukkitPlayers) {
                    PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());
                    if (practicePlayer != null) {
                        practicePlayer.setCurrentQueue(null);
                    }
                }

                try {
                    // Create FFA match
                    matchManager.createFFAMatch(bukkitPlayers, ffaQueue.getLadder(),
                            plugin.getArenaManager().getAvailableArena(), ffaQueue.isRanked());
                } catch (Exception e) {
                    e.printStackTrace();
                    // If match creation fails, notify players
                    for (Player player : bukkitPlayers) {
                        player.sendMessage("§cFailed to create FFA match. Please try again.");
                    }
                }
            }
        }.runTask(plugin);
    }
}