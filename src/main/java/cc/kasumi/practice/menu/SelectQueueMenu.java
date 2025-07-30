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
import cc.kasumi.practice.player.PlayerState;
import cc.kasumi.practice.player.PracticePlayer;
import cc.kasumi.practice.util.GameUtil;
import lombok.AllArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static cc.kasumi.practice.PracticeConfiguration.*;

public class SelectQueueMenu extends Menu {

    private boolean ranked;

    {
        setAutoUpdate(true);
    }

    public SelectQueueMenu(boolean ranked) {
        this.ranked = ranked;
    }

    @Override
    public String getTitle(Player player) {
        return (this.ranked ? CC.GREEN + "Ranked" : CC.BLUE + "Unranked") + " queue";
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (Queue queue : Practice.getInstance().getQueueManager().getQueues().values()) {
            if (queue.isRanked() != this.ranked) continue;

            buttons.put(queue.getLadder().getDisplaySlot() - 1, new SelectLadderButton(queue));
        }

        return buttons;
    }

    @AllArgsConstructor
    private class SelectLadderButton extends Button {

        private Queue queue;

        @Override
        public ItemStack getButtonItem(Player player) {
            Ladder ladder = queue.getLadder();

            TypeData typeData = ladder.getDisplayType();

            return new ItemBuilder(
                    typeData.getType(), typeData.getData()).
                    name(MAIN_COLOR + ladder.getDisplayName()).
                    lore(CC.YELLOW + "Right click to join queue").
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

            player.closeInventory();

            queue.getPlayers().add(new QueuePlayer(player.getUniqueId()));
            practicePlayer.setPlayerState(PlayerState.QUEUEING);
            practicePlayer.setCurrentQueue(queue);
            player.sendMessage(MAIN_COLOR + "Added you to " + (queue.isRanked() ? "ranked " : "unranked ") + SEC_COLOR + queue.getLadder().getDisplayName() + MAIN_COLOR + " queue!");
            player.getOpenInventory().close();
            player.getInventory().setContents(GameUtil.getQueueContents());
        }
    }
}
