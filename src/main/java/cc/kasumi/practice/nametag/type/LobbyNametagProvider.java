package cc.kasumi.practice.nametag.type;

import cc.kasumi.practice.nametag.NametagContent;
import cc.kasumi.practice.nametag.NametagProvider;
import cc.kasumi.practice.nametag.PlayerNametag;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.NametagUtil;
import org.bukkit.entity.Player;

public class LobbyNametagProvider implements NametagProvider {

    @Override
    public NametagContent getContent(PlayerNametag playerNametag, PracticePlayer practicePlayer) {
        Player player = playerNametag.getPlayer();

        // Check for special permissions
        if (player.hasPermission("practice.admin")) {
            return NametagUtil.createAdminContent();
        }

        if (practicePlayer.isBuilder()) {
            return NametagUtil.createBuilderContent();
        }

        // Default lobby nametag with ELO
        return NametagUtil.createLobbyContent(practicePlayer);
    }
}