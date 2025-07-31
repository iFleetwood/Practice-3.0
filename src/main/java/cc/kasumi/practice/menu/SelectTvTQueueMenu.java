package cc.kasumi.practice.menu;

import cc.kasumi.commons.menu.Button;
import cc.kasumi.commons.menu.Menu;
import cc.kasumi.commons.util.CC;
import cc.kasumi.commons.util.ItemBuilder;
import cc.kasumi.commons.util.TypeData;
import cc.kasumi.practice.Practice;
import cc.kasumi.practice.game.ladder.Ladder;
import cc.kasumi.practice.game.queue.Queue;
import cc.kasumi.practice.game.queue.QueuePlayer;
import cc.kasumi.practice.game.queue.QueueType;
import cc.kasumi.practice.game.queue.type.PartyQueue;
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class SelectTvTQueueMenu extends Menu {

    private boolean ranked;

    {
        setAutoUpdate(true);
    }

    public SelectTvTQueueMenu(boolean ranked) {
        this.ranked = ranked;
    }

    @Override
    public String getTitle(Player player) {
        return (this.ranked ? CC.GREEN + "Ranked" : CC.BLUE + "Unranked") + " TvT Queue";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Queue queue : Practice.getInstance().getQueueManager().getQueues().values()) {
            if (queue.getType() != QueueType.PARTY) continue;
            if (queue.isRanked() != this.ranked) continue;

            buttons.put(queue.getLadder().getDisplaySlot() - 1, new SelectTvTLadderButton((PartyQueue) queue));
        }

        return buttons;
    }

    @AllArgsConstructor
    private class SelectTvTLadderButton extends Button {

        private PartyQueue queue;

        @Override
        public ItemStack getButtonItem(Player player) {
            Ladder ladder = queue.getLadder();
            TypeData typeData = ladder.getDisplayType();

            return new ItemBuilder(
                    typeData.getType(), typeData.getData()).
                    name(MAIN_COLOR + ladder.getDisplayName() + " " + queue.getPartySize() + "v" + queue.getPartySize()).
                    lore(Arrays.asList(
                            CC.YELLOW + "Right click to join TvT queue",
                            "",
                            CC.GRAY + "Queue status: " + CC.WHITE + queue.getQueueStatus(),
                            CC.GRAY + "Minimum players: " + CC.WHITE + queue.getMinPlayers(),
                            "",
                            queue.getReadyStatusMessage()
                    )).
                    flag(ItemFlag.HIDE_POTION_EFFECTS).build();
        }

        @Override
        public void clicked(Player player, ClickType clickType) {
            PracticePlayer practicePlayer = PracticePlayer.getPracticePlayer(player.getUniqueId());

            if (practicePlayer == null) {
                return;
            }

            if (practicePlayer.isInMatch()) {
                player.sendMessage(ERROR_COLOR + "You cannot queue while in match");
                return;
            }

            if (queue.isFull()) {
                player.sendMessage(ERROR_COLOR + "This TvT queue is currently full!");
                return;
            }

            player.closeInventory();

            queue.addPlayer(new QueuePlayer(player.getUniqueId()));
            practicePlayer.setPlayerState(PlayerState.QUEUEING);
            practicePlayer.setCurrentQueue(queue);

            String queueType = queue.isRanked() ? "ranked " : "unranked ";
            player.sendMessage(MAIN_COLOR + "Added you to " + queueType + SEC_COLOR + queue.getLadder().getDisplayName() +
                    MAIN_COLOR + " " + queue.getPartySize() + "v" + queue.getPartySize() + " queue! " + 
                    SEC_COLOR + "(" + queue.getQueueStatus() + ")");

            player.getInventory().setContents(GameUtil.getQueueContents());
        }
    }
}