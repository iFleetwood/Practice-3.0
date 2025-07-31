package cc.kasumi.practice.nametag;

import org.bukkit.scheduler.BukkitRunnable;

public class NametagRunnable extends BukkitRunnable {

    private final NametagManager manager;

    public NametagRunnable(NametagManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        manager.getNametags().forEach((uuid, nametag) -> {
            try {
                manager.updateNametag(nametag);
            } catch (Exception e) {
                // Silently handle errors to prevent spam
            }
        });
    }
}