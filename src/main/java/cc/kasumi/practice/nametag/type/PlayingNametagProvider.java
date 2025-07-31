package cc.kasumi.practice.nametag.type;

import cc.kasumi.practice.nametag.NametagContent;
import cc.kasumi.practice.nametag.NametagProvider;
import cc.kasumi.practice.nametag.PlayerNametag;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.NametagUtil;
import org.bukkit.entity.Player;

public class PlayingNametagProvider implements NametagProvider {

    @Override
    public NametagContent getContent(PlayerNametag playerNametag, PracticePlayer practicePlayer) {
        Player player = playerNametag.getPlayer();

        // Show health and food during matches
        return NametagUtil.createMatchContent(player);
    }
}
