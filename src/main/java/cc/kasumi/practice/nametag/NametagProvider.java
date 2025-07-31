package cc.kasumi.practice.nametag;

import cc.kasumi.practice.player.PracticePlayer;

public interface NametagProvider {

    NametagContent getContent(PlayerNametag playerNametag, PracticePlayer practicePlayer);
}