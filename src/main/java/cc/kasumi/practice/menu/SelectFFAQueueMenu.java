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
import cc.kasumi.practice.game.queue.type.FFAQueue;
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

public class SelectFFAQueueMenu extends Menu {

    private boolean ranked;

    {
        setAutoUpdate(true);
    }

    public SelectFFAQueueMenu(boolean ranked) {
        this.ranked = ranked;
    }

    @Override
    public String getTitle(Player player) {
        return (this.ranked ? CC.GREEN + "Ranked" : CC.BLUE + "Unranked") + " FFA Queue";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Queue queue : Practice.getInstance().getQueueManager().getQueues().values()) {
            if (queue.getType() != QueueType.FFA) continue;
            if (queue.isRanked() != this.ranked) continue;

            buttons.put(queue.getLadder().getDisplaySlot() - 1, new SelectFFALadderButton((FFAQueue) queue));
        }

        return buttons;
    }

    @AllArgsConstructor
    private class SelectFFALadderButton extends Button {

        private FFAQueue queue;

        @Override
        public ItemStack getButtonItem(Player player) {
            Ladder ladder = queue.getLadder();
            TypeData typeData = ladder.getDisplayType();

            return new ItemBuilder(
                    typeData.getType(), typeData.getData()).
                    name(MAIN_COLOR + ladder.getDisplayName() + " FFA").
                    lore(Arrays.asList(
                            CC.YELLOW + "Right click to join FFA queue",
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
                player.sendMessage(ERROR_COLOR + "This FFA queue is currently full!");
                return;
            }

            player.closeInventory();

            QueuePlayer queuePlayer;
            if (queue.isRanked()) {
                int ladderRating = practicePlayer.getLadderElo(queue.getLadder()).getRating();
                queuePlayer = new QueuePlayer(player.getUniqueId(), ladderRating);
                // Reset notification tracking to prevent immediate spam
                queuePlayer.resetNotificationTracking();
            } else {
                queuePlayer = new QueuePlayer(player.getUniqueId());
            }

            queue.addPlayer(queuePlayer);
            practicePlayer.setPlayerState(PlayerState.QUEUEING);
            practicePlayer.setCurrentQueue(queue);

            if (queue.isRanked()) {
                player.sendMessage(MAIN_COLOR + "Added you to ranked " + SEC_COLOR + queue.getLadder().getDisplayName() +
                        MAIN_COLOR + " FFA queue! " + SEC_COLOR + "(" + queue.getQueueStatus() + ")");
                player.sendMessage("§7ELO: §f" + queuePlayer.getRating() + " §7| Search Range: §f±" + queuePlayer.getRange() + " §7(expands over time)");
            } else {
                player.sendMessage(MAIN_COLOR + "Added you to unranked " + SEC_COLOR + queue.getLadder().getDisplayName() +
                        MAIN_COLOR + " FFA queue! " + SEC_COLOR + "(" + queue.getQueueStatus() + ")");
            }

            player.getInventory().setContents(GameUtil.getQueueContents());
        }
    }
}