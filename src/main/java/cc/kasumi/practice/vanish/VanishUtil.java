package cc.kasumi.practice.vanish;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class VanishUtil {

    public static void showAllPlayers(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            player.showPlayer(onlinePlayer);
            onlinePlayer.showPlayer(player);
        }
    }

    public static void hideAllPlayers(Player player) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer == player) continue;

            player.hidePlayer(onlinePlayer);
            onlinePlayer.hidePlayer(player);
        }
    }
}
