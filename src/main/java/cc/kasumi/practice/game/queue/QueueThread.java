package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.match.MatchManager;
import cc.kasumi.practice.player.PlayerElo;
import cc.kasumi.practice.player.PracticePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

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
                    if (queue.getPlayers().size() < 2) {
                        continue;
                    }

                    List<QueuePlayer> queuePlayers = queue.getPlayers();

                    for (QueuePlayer firstQueuePlayer : queuePlayers) {
                        Player firstPlayer = firstQueuePlayer.getPlayer();

                        if (firstPlayer == null) {
                            continue;
                        }

                        for (QueuePlayer secondQueuePlayer : queuePlayers) {
                            if (firstQueuePlayer.equals(secondQueuePlayer)) {
                                continue;
                            }

                            Player secondPlayer = secondQueuePlayer.getPlayer();

                            if (secondPlayer == null) {
                                continue;
                            }

                            // Just check if in range
                            if (queue.isRanked()) if (!firstQueuePlayer.isInRange(secondQueuePlayer.getRating()) ||
                                    !secondQueuePlayer.isInRange(firstQueuePlayer.getRange())) {
                                continue;
                            }

                            queuePlayers.remove(firstQueuePlayer);
                            queuePlayers.remove(secondQueuePlayer);

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    PracticePlayer firstPracticePlayer = PracticePlayer.getPracticePlayer(firstPlayer.getUniqueId());
                                    PracticePlayer secondPracticePlayer = PracticePlayer.getPracticePlayer(secondPlayer.getUniqueId());
                                    firstPracticePlayer.setCurrentQueue(null);
                                    secondPracticePlayer.setCurrentQueue(null);

                                    matchManager.createSoloMatch(firstPlayer, secondPlayer, queue.getLadder(), plugin.getArenaManager().getAvailableArena(), queue.isRanked());
                                }
                            }.runTask(plugin);
                        }
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
}