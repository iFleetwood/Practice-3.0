package cc.kasumi.practice.nametag.type;

import cc.kasumi.practice.nametag.NametagContent;
import cc.kasumi.practice.nametag.NametagProvider;
import cc.kasumi.practice.nametag.PlayerNametag;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.NametagUtil;

public class QueueingNametagProvider implements NametagProvider {

    @Override
    public NametagContent getContent(PlayerNametag playerNametag, PracticePlayer practicePlayer) {
        return NametagUtil.createQueueContent();
    }
}