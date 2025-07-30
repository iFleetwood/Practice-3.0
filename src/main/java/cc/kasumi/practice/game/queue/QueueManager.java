package cc.kasumi.practice.game.queue;

import cc.kasumi.practice.Practice;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class QueueManager {

    private final Map<String, Queue> queues = new ConcurrentHashMap<>();
    private final QueueThread queueThread;

    public QueueManager(Practice plugin) {
        this.queueThread = new QueueThread(plugin, this);
        this.queueThread.start();
    }
}
