package cc.kasumi.practice.scoreboard;

import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardRunnable extends BukkitRunnable {

    private final ScoreboardManager manager;

    public ScoreboardRunnable(ScoreboardManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.getScoreboards().forEach((uuid, scoreboard) -> manager.updateScoreboard(scoreboard));
    }
}

